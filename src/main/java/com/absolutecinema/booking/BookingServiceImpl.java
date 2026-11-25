package com.absolutecinema.booking;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.absolutecinema.auth.User;
import com.absolutecinema.catalog.Seat;
import com.absolutecinema.catalog.Showtime;
import com.absolutecinema.catalog.ShowtimeRepository;

@Service
public class BookingServiceImpl implements BookingService {

    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;

    public BookingServiceImpl(ShowtimeSeatRepository showtimeSeatRepository,
                              BookingRepository bookingRepository,
                              ShowtimeRepository showtimeRepository) {
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.bookingRepository = bookingRepository;
        this.showtimeRepository = showtimeRepository;
    }

    // ===================== STAGE 1: reads =====================

    @Override
    @Transactional(readOnly = true)
    public Showtime getShowtime(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Showtime not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatView> getSeatMap(Long showtimeId) {
        LocalDateTime now = LocalDateTime.now();
        return showtimeSeatRepository.findMap(showtimeId).stream()
                .map(ss -> new SeatView(
                        ss.getSeat().getId(),
                        ss.getSeat().getRowLabel(),
                        ss.getSeat().getSeatNumber(),
                        effectiveStatus(ss, now)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBooking(Long bookingId, User user) {
        return bookingRepository.findById(bookingId)
                .filter(b -> b.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(User user) {
        return bookingRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSeatLabels(Long bookingId) {
        return showtimeSeatRepository.findByBookingId(bookingId).stream()
                .map(ShowtimeSeat::getSeat)
                .sorted(Comparator.comparing(Seat::getRowLabel).thenComparingInt(Seat::getSeatNumber))
                .map(BookingServiceImpl::label)
                .toList();
    }

    // ===================== STAGE 2: lock =====================

    private static final int LOCK_MINUTES = 5;
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public Booking lockSeats(Long showtimeId, List<Long> seatIds, User user) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un asiento.");
        }
        Set<Long> ids = new LinkedHashSet<>(seatIds);
        if (ids.size() > MAX_SEATS_PER_BOOKING) {
            throw new IllegalArgumentException(
                    "Máximo " + MAX_SEATS_PER_BOOKING + " asientos por reserva.");
        }

        Showtime showtime = getShowtime(showtimeId);
        LocalDateTime now = LocalDateTime.now();
        if (!showtime.getStartsAt().isAfter(now)) {
            throw new IllegalArgumentException("Esta función ya comenzó.");
        }

        // A new attempt replaces the user's previous pending selection for this showtime
        cancelPending(showtimeId, user);

        // SELECT ... FOR UPDATE ordered by seat id: concurrent requests wait here
        List<ShowtimeSeat> seats = showtimeSeatRepository.findForUpdate(showtimeId, ids);
        if (seats.size() != ids.size()) {
            throw new IllegalArgumentException("Hay asientos que no pertenecen a esta función.");
        }

        List<String> taken = seats.stream()
                .filter(ss -> !isFree(ss, now))
                .map(ss -> label(ss.getSeat()))
                .sorted()
                .toList();
        if (!taken.isEmpty()) {
            throw new SeatsUnavailableException(taken);   // rolls back: nothing is locked
        }

        LocalDateTime expiresAt = now.plusMinutes(LOCK_MINUTES);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotal(showtime.getPrice().multiply(BigDecimal.valueOf(seats.size())));
        booking.setCode(newCode(now));
        booking.setCreatedAt(now);
        booking.setExpiresAt(expiresAt);
        booking = bookingRepository.save(booking);

        for (ShowtimeSeat ss : seats) {
            ss.setStatus(SeatStatus.LOCKED);
            ss.setLockedBy(user);
            ss.setLockExpiresAt(expiresAt);
            ss.setBooking(booking);
        }
        return booking;
    }

    @Override
    @Transactional
    public void cancelPending(Long showtimeId, User user) {
        List<Booking> pending = bookingRepository.findByUserIdAndShowtimeIdAndStatus(
                user.getId(), showtimeId, BookingStatus.PENDING);
        for (Booking b : pending) {
            for (ShowtimeSeat ss : showtimeSeatRepository.findByBookingIdForUpdate(b.getId())) {
                if (ss.getStatus() == SeatStatus.LOCKED) {
                    release(ss);
                }
            }
            b.setStatus(BookingStatus.INCOMPLETE);
        }
    }

    // ===================== STAGE 3: confirm + expiry =====================

    /**
     * The only place that marks seats as SOLD. Joins the caller's transaction (payment).
     * Throws IllegalStateException if the booking is not PENDING, already expired,
     * or its seats are no longer locked by it.
     */
    @Override
    @Transactional
    public Booking confirm(Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        LocalDateTime now = LocalDateTime.now();

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("La reserva no está pendiente.");
        }
        if (!booking.getExpiresAt().isAfter(now)) {
            throw new IllegalStateException("La reserva venció.");
        }

        List<ShowtimeSeat> seats = showtimeSeatRepository.findByBookingIdForUpdate(bookingId);
        if (seats.isEmpty() || seats.stream().anyMatch(ss -> ss.getStatus() != SeatStatus.LOCKED)) {
            throw new IllegalStateException("Los asientos de la reserva ya no están bloqueados.");
        }

        for (ShowtimeSeat ss : seats) {
            ss.setStatus(SeatStatus.SOLD);
            ss.setLockedBy(null);
            ss.setLockExpiresAt(null);
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        return booking;
    }

    @Override
    @Transactional
    public int releaseExpired() {
        LocalDateTime now = LocalDateTime.now();
        int freed = showtimeSeatRepository.releaseExpired(now);
        bookingRepository.expirePending(now);
        return freed;
    }

    // ===================== helpers =====================

    private static boolean isFree(ShowtimeSeat ss, LocalDateTime now) {
        return ss.getStatus() == SeatStatus.AVAILABLE
                || (ss.getStatus() == SeatStatus.LOCKED
                    && ss.getLockExpiresAt() != null
                    && ss.getLockExpiresAt().isBefore(now));
    }

    private static SeatStatus effectiveStatus(ShowtimeSeat ss, LocalDateTime now) {
        return isFree(ss, now) ? SeatStatus.AVAILABLE : ss.getStatus();
    }

    private static void release(ShowtimeSeat ss) {
        ss.setStatus(SeatStatus.AVAILABLE);
        ss.setLockedBy(null);
        ss.setLockExpiresAt(null);
        ss.setBooking(null);
    }

    /** e.g. "ABC-241126-K7M2" (letters - ddMMyy - 4 random chars). */
    private String newCode(LocalDateTime now) {
        String code;
        do {
            code = randomChars(3) + "-" + now.format(DateTimeFormatter.ofPattern("ddMMyy"))
                    + "-" + randomChars(4);
        } while (bookingRepository.existsByCode(code));
        return code;
    }

    private static String randomChars(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private static String label(Seat seat) {
        return seat.getRowLabel() + seat.getSeatNumber();
    }
}
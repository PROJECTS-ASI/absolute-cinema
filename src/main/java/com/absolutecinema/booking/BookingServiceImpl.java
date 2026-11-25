package com.absolutecinema.booking;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

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

    // ===================== STAGE 2 (pending) =====================

    @Override
    public Booking lockSeats(Long showtimeId, List<Long> seatIds, User user) {
        throw new UnsupportedOperationException("TODO: stage 2");
    }

    @Override
    public void cancelPending(Long showtimeId, User user) {
        // TODO: stage 2. No-op for now so the seat map screen works.
    }

    // ===================== STAGE 3 (pending) =====================

    @Override
    public Booking confirm(Long bookingId) {
        throw new UnsupportedOperationException("TODO: stage 3");
    }

    @Override
    public int releaseExpired() {
        return 0;   // TODO: stage 3
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

    private static String label(Seat seat) {
        return seat.getRowLabel() + seat.getSeatNumber();
    }
}
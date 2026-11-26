package com.absolutecinema.profile;


import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.absolutecinema.auth.User;
import com.absolutecinema.auth.UserService;
import com.absolutecinema.booking.Booking;
import com.absolutecinema.booking.BookingRepository;
import com.absolutecinema.booking.ShowtimeSeat;
import com.absolutecinema.booking.ShowtimeSeatRepository;
import com.absolutecinema.payment.Payment;
import com.absolutecinema.payment.PaymentRepository;
import com.absolutecinema.booking.BookingStatus;

@Service
public class ProfileService {

    private final UserService userService;
    private final BookingRepository bookingRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final PaymentRepository paymentRepository;

    public ProfileService(
            UserService userService,
            BookingRepository bookingRepository,
            ShowtimeSeatRepository showtimeSeatRepository,
            PaymentRepository paymentRepository) {

        this.userService = userService;
        this.bookingRepository = bookingRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.paymentRepository = paymentRepository;
    }

    // ==============================
    // PERFIL DE USUARIO
    // ==============================

    @Transactional(readOnly = true)
    public User getUserProfile(String email) {
        return userService.getByEmail(email);
    }

    @Transactional
    public User updateProfile(String email, ProfileUpdateRequest request) {

        User user = userService.getByEmail(email);

        user.setName(request.getName().trim());
        user.setLastName(request.getLastName().trim());
        user.setAge(request.getAge());

        user.setCellphone(
                request.getCellphone() == null || request.getCellphone().isBlank()
                        ? null
                        : request.getCellphone().trim()
        );

        user.setCity(
                request.getCity() == null || request.getCity().isBlank()
                        ? null
                        : request.getCity().trim()
        );

        return userService.save(user);
    }

    // ==============================
    // HISTORIAL DE COMPRAS
    // ==============================

    @Transactional(readOnly = true)
    public List<PurchaseHistoryItem> getPurchaseHistory(String email) {

        User user = userService.getByEmail(email);

        List<Booking> bookings =
                bookingRepository.findByUserOrderByCreatedAtDesc(user);

        return bookings.stream()
        		.filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .map(booking -> {

                    List<ShowtimeSeat> seats =
                            showtimeSeatRepository.findByBookingId(booking.getId());

                    String seatLabels = seats.stream()
                            .map(showtimeSeat ->
                                    showtimeSeat.getSeat().getRowLabel()
                                    + showtimeSeat.getSeat().getSeatNumber())
                            .sorted()
                            .collect(Collectors.joining(", "));

                    List<Payment> payments =
                            paymentRepository.findByBookingId(booking.getId());

                    var latestPayment = payments.stream()
                            .max(Comparator.comparing(Payment::getCreatedAt));

                    PurchaseHistoryItem item = new PurchaseHistoryItem();

                    item.setBookingId(booking.getId());
                    item.setCode(booking.getCode());
                    item.setMovieTitle(
                            booking.getShowtime().getMovie().getTitle());
                    item.setShowtimeDate(
                            booking.getShowtime().getStartsAt());
                    item.setPurchaseDate(booking.getCreatedAt());
                    item.setSeats(seatLabels);
                    item.setTotal(booking.getTotal());
                    item.setBookingStatus(booking.getStatus());

                    latestPayment.ifPresent(payment ->
                            item.setPaymentStatus(payment.getStatus()));

                    return item;
                })
                .collect(Collectors.toList());
    }

    // ==============================
    // DETALLE DE COMPRA
    // ==============================

    @Transactional(readOnly = true)
    public PurchaseDetailItem getPurchaseDetail(String email, Long bookingId) {

        User user = userService.getByEmail(email);

        Booking booking = bookingRepository.findById(bookingId)
                .filter(foundBooking ->
                        foundBooking.getUser().getId().equals(user.getId())
                        && foundBooking.getStatus() == BookingStatus.CONFIRMED)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Obtener asientos de la reserva
        List<ShowtimeSeat> seats =
                showtimeSeatRepository.findByBookingId(booking.getId());

        String seatLabels = seats.stream()
                .map(showtimeSeat ->
                        showtimeSeat.getSeat().getRowLabel()
                        + showtimeSeat.getSeat().getSeatNumber())
                .sorted()
                .collect(Collectors.joining(", "));

        // Obtener todos los pagos de la reserva
        List<PurchasePaymentItem> paymentItems =
                paymentRepository.findByBookingId(booking.getId())
                        .stream()
                        .sorted(Comparator.comparing(Payment::getCreatedAt))
                        .map(this::toPurchasePaymentItem)
                        .collect(Collectors.toList());

        // Construir el detalle
        PurchaseDetailItem detail = new PurchaseDetailItem();

        detail.setBookingId(booking.getId());
        detail.setCode(booking.getCode());
        detail.setMovieTitle(
                booking.getShowtime().getMovie().getTitle());
        detail.setShowtimeDate(
                booking.getShowtime().getStartsAt());
        detail.setPurchaseDate(booking.getCreatedAt());
        detail.setSeats(seatLabels);
        detail.setTotal(booking.getTotal());
        detail.setBookingStatus(booking.getStatus());
        detail.setPayments(paymentItems);

        return detail;
    }

    // ==============================
    // CONVERSIÓN DE PAGOS A DTO
    // ==============================

    private PurchasePaymentItem toPurchasePaymentItem(Payment payment) {

        PurchasePaymentItem item = new PurchasePaymentItem();

        item.setPaymentId(payment.getId());
        item.setMethod(payment.getMethod());
        item.setStatus(payment.getStatus());
        item.setAmount(payment.getAmount());
        item.setCardBrand(payment.getCardBrand());
        item.setRejectionReason(payment.getRejectionReason());
        item.setCreatedAt(payment.getCreatedAt());

        return item;
    }
}


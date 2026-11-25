package com.absolutecinema.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.absolutecinema.booking.Booking;
import com.absolutecinema.booking.BookingRepository;
import com.absolutecinema.booking.BookingService;
import com.absolutecinema.booking.BookingStatus;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final CardValidator cardValidator;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            BookingService bookingService,
            CardValidator cardValidator) {

        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.cardValidator = cardValidator;
    }

    @Override
    @Transactional
    public Payment registerPayment(
            Long bookingId,
            PaymentMethod method,
            BigDecimal amount) {

        Booking booking = validateBooking(bookingId);

        if (method == null || amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Datos de pago inválidos");
        }

        validateAmount(booking, amount);

        if (method != PaymentMethod.MANUAL_AMOUNT) {
            throw new IllegalArgumentException(
                    "Este método requiere datos de tarjeta");
        }

        return approvePayment(booking, method, amount, null);
    }

    @Override
    @Transactional
    public Payment registerCardPayment(
            Long bookingId,
            PaymentRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Los datos de la tarjeta son obligatorios");
        }

        Booking booking = validateBooking(bookingId);

        BigDecimal amount = request.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Importe inválido");
        }

        validateAmount(booking, amount);

        String brand = cardValidator.validate(request);

        // El pago es simulado: no se almacena ningún dato sensible.
        return approvePayment(
                booking,
                PaymentMethod.CARD,
                amount,
                brand
        );
    }

    private Booking validateBooking(Long bookingId) {

        if (bookingId == null) {
            throw new IllegalArgumentException(
                    "El ID de la reserva es obligatorio");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Reserva no encontrada"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException(
                    "La reserva no está pendiente");
        }

        if (booking.getExpiresAt() == null ||
                !LocalDateTime.now().isBefore(booking.getExpiresAt())) {
            throw new IllegalArgumentException(
                    "La reserva ha vencido");
        }

        return booking;
    }

    private void validateAmount(Booking booking, BigDecimal amount) {

        if (booking.getTotal() == null ||
                amount.compareTo(booking.getTotal()) != 0) {
            throw new IllegalArgumentException(
                    "El importe no coincide con el total de la reserva");
        }
    }

    private Payment approvePayment(
            Booking booking,
            PaymentMethod method,
            BigDecimal amount,
            String brand) {

        Payment payment = new Payment();

        payment.setBooking(booking);
        payment.setMethod(method);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setCardBrand(brand);
        payment.setCreatedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        // Confirmación mediante el servicio de reservas.
        bookingService.confirm(booking.getId());

        return savedPayment;
    }

    @Override
    public Payment findById(Long id) {

        return paymentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Pago no encontrado"));
    }

    @Override
    public List<Payment> findByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    @Override
    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }
}

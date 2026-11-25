package com.absolutecinema.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.absolutecinema.booking.Booking;
import com.absolutecinema.booking.BookingRepository;
import com.absolutecinema.booking.BookingService;
import com.absolutecinema.booking.BookingStatus;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal MAX_STORED_AMOUNT = new BigDecimal("99999999");

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final CardValidator cardValidator;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
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
    public Payment pay(Long bookingId, PaymentRequest request) {
        if (request == null || request.getMethod() == null) {
            throw new IllegalArgumentException("Selecciona un método de pago.");
        }

        // Se bloquea la reserva desde el inicio: un doble clic no puede pagar dos veces
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        // Estado y vencimiento (no se registra pago: el controlador redirige)
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("La reserva no está pendiente.");
        }
        if (!booking.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("La reserva venció.");
        }

        PaymentMethod method = request.getMethod();
        String brand = null;

        try {
            if (method == PaymentMethod.MANUAL_AMOUNT) {
                validateManualAmount(booking, request.getAmount());          // RN-12
            } else {
                brand = cardValidator.validate(request);                    // RN-17
            }
        } catch (IllegalArgumentException e) {
            // RN-15: se registra el rechazo, la reserva sigue PENDING y se puede reintentar
            return save(booking, method, rejectedAmount(booking, request), null,
                    PaymentStatus.REJECTED, e.getMessage());
        }

        // Aprobado. Con tarjeta el importe siempre es el total de la reserva.
        Payment payment = save(booking, method, booking.getTotal(), brand,
                PaymentStatus.APPROVED, null);

        // Misma transacción: si confirm falla, el pago se revierte también
        bookingService.confirm(booking.getId());
        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    // ---------------------------------------------------------------

    private void validateManualAmount(Booking booking, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ingresa un importe válido.");
        }
        if (amount.compareTo(booking.getTotal()) != 0) {
            throw new IllegalArgumentException(
                    "El importe no coincide con el total de la reserva (S/ "
                            + booking.getTotal().setScale(2) + ").");
        }
    }

    /** Importe a guardar en un pago rechazado: lo digitado si es razonable; si no, el total. */
    private BigDecimal rejectedAmount(Booking booking, PaymentRequest request) {
        BigDecimal typed = request.getAmount();
        if (request.getMethod() == PaymentMethod.MANUAL_AMOUNT
                && typed != null
                && typed.compareTo(BigDecimal.ZERO) > 0
                && typed.compareTo(MAX_STORED_AMOUNT) < 0) {
            return typed.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return booking.getTotal();
    }

    // Nunca se guarda número de tarjeta, CVV ni DNI (RNF-12)
    private Payment save(Booking booking, PaymentMethod method, BigDecimal amount,
                         String brand, PaymentStatus status, String reason) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setMethod(method);
        payment.setAmount(amount);
        payment.setStatus(status);
        payment.setCardBrand(brand);
        payment.setRejectionReason(reason);
        payment.setCreatedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }
}

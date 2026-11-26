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
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final CardValidator cardValidator;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          BookingService bookingService,
                          CardValidator cardValidator) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.cardValidator = cardValidator;
    }

    /**
     * Procesa el pago y confirma la reserva en la misma transacción.
     * - IllegalArgumentException: datos de pago inválidos (se rechaza, la reserva sigue PENDING).
     * - IllegalStateException: reserva vencida o ya no pendiente.
     */
    @Transactional
    public Payment pay(Long bookingId, PaymentRequest request) {
        if (bookingId == null) {
            throw new IllegalArgumentException("El ID de la reserva es obligatorio");
        }
        if (request == null || request.getMethod() == null) {
            throw new IllegalArgumentException("El método de pago es obligatorio");
        }

        // Se bloquea la reserva desde el inicio: evita doble pago por doble envío
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("La reserva no está pendiente");
        }
        if (booking.getExpiresAt() == null || !LocalDateTime.now().isBefore(booking.getExpiresAt())) {
            throw new IllegalStateException("La reserva ha vencido");
        }

        BigDecimal amount;
        String brand = null;

        if (request.getMethod() == PaymentMethod.MANUAL_AMOUNT) {
            amount = request.getAmount();                                   // RN-12
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El importe debe ser mayor que cero");
            }
            if (booking.getTotal() == null || amount.compareTo(booking.getTotal()) != 0) {
                throw new IllegalArgumentException("El importe no coincide con el total de la reserva");
            }
        } else {
            brand = cardValidator.validate(request);                        // RN-17
            amount = booking.getTotal();                                    // el importe lo define el servidor
        }

        if (!paymentRepository.findByBookingId(bookingId).isEmpty()) {
            throw new IllegalArgumentException("Esta reserva ya tiene un pago registrado");
        }

        // Asientos LOCKED -> SOLD y reserva CONFIRMED (si falla, no se guarda el pago)
        Booking confirmed = bookingService.confirm(bookingId);

        Payment payment = new Payment();
        payment.setBooking(confirmed);
        payment.setMethod(request.getMethod());
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setCardBrand(brand);                                        // "VISA" / "AMEX" o null
        payment.setCreatedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    public List<Payment> findByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }
}
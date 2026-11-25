
package com.absolutecinema.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.absolutecinema.booking.Booking;
import com.absolutecinema.booking.BookingRepository;
import com.absolutecinema.booking.BookingStatus;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository) {

        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    // Registrar un nuevo pago
    @Transactional
    public Payment registerPayment(
            Long bookingId,
            PaymentMethod method,
            BigDecimal amount) {

        // Validar ID de reserva
        if (bookingId == null) {
            throw new IllegalArgumentException(
                "El ID de la reserva es obligatorio");
        }

        // Buscar reserva
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Reserva no encontrada"));

        // Verificar estado de reserva
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException(
                "La reserva no está pendiente");
        }

        // Verificar vencimiento
        if (booking.getExpiresAt() == null ||
            !LocalDateTime.now().isBefore(booking.getExpiresAt())) {

            throw new IllegalArgumentException(
                "La reserva ha vencido");
        }

        // Validar método de pago
        if (method == null) {
            throw new IllegalArgumentException(
                "El método de pago es obligatorio");
        }

        // Validar importe
        if (amount == null ||
            amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                "El importe debe ser mayor que cero");
        }

        // Comprobar que el importe coincida con la reserva
        if (booking.getTotal() == null ||
            amount.compareTo(booking.getTotal()) != 0) {

            throw new IllegalArgumentException(
                "El importe no coincide con el total de la reserva");
        }

        // Evitar pagos duplicados
        List<Payment> existingPayments =
                paymentRepository.findByBookingId(bookingId);

        if (!existingPayments.isEmpty()) {
            throw new IllegalArgumentException(
                "Esta reserva ya tiene un pago registrado");
        }

        // Crear el registro del pago
        Payment payment = new Payment();

        payment.setBooking(booking);
        payment.setMethod(method);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setCreatedAt(LocalDateTime.now());

        // Guardar el pago
        Payment savedPayment = paymentRepository.save(payment);

        // Confirmar la reserva
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        return savedPayment;
    }

    // Buscar pago por identificador
    public Payment findById(Long id) {

        return paymentRepository.findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Pago no encontrado"));
    }

    // Consultar pagos de una reserva
    public List<Payment> findByBooking(Long bookingId) {

        return paymentRepository.findByBookingId(bookingId);
    }

    // Listar todos los pagos
    public List<Payment> findAll() {

        return paymentRepository.findAll();
    }
}
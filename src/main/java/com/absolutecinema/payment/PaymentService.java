package com.absolutecinema.payment;

import java.util.List;

public interface PaymentService {

    /**
     * Procesa un pago para la reserva.
     * - Pago aprobado: registra el Payment (APPROVED) y llama a bookingService.confirm()
     *   en la misma transacción.
     * - Pago rechazado (importe o tarjeta inválidos): registra el Payment (REJECTED) con
     *   su motivo, deja la reserva en PENDING y lo devuelve sin lanzar excepción.
     * - Lanza IllegalStateException si la reserva no está PENDING o ya venció.
     */
    Payment pay(Long bookingId, PaymentRequest request);

    List<Payment> findByBooking(Long bookingId);
}

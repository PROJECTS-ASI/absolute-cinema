package com.absolutecinema.payment;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {

    Payment registerPayment(
            Long bookingId,
            PaymentMethod method,
            BigDecimal amount
    );

    Payment registerCardPayment(
            Long bookingId,
            PaymentRequest request
    );

    Payment findById(Long id);

    List<Payment> findByBooking(Long bookingId);

    List<Payment> findAll();
}

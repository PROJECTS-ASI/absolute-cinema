package com.absolutecinema.payment;

import java.math.BigDecimal;

public record PaymentResponse(Long id, String status, String method,
                              BigDecimal amount, String cardBrand) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getStatus().name(), p.getMethod().name(),
                p.getAmount(), p.getCardBrand());
    }
}
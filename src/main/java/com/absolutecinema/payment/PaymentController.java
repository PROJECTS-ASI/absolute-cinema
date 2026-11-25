package com.absolutecinema.payment;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Registrar un pago
    @PostMapping
    public Payment registerPayment(@RequestBody PaymentRequest request) {
        try {
            return paymentService.registerPayment(
                request.getBookingId(),
                request.getMethod(),
                request.getAmount()
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // Consultar un pago por ID
    @GetMapping("/{id}")
    public Payment findById(@PathVariable Long id) {
        try {
            return paymentService.findById(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // Consultar pagos de una reserva
    @GetMapping("/booking/{bookingId}")
    public List<Payment> findByBooking(
            @PathVariable Long bookingId) {
        return paymentService.findByBooking(bookingId);
    }

    // Listar todos los pagos
    @GetMapping
    public List<Payment> findAll() {
        return paymentService.findAll();
    }

    // Datos que recibe el pago
    public static class PaymentRequest {

        private Long bookingId;
        private PaymentMethod method;
        private BigDecimal amount;

        public Long getBookingId() {
            return bookingId;
        }

        public void setBookingId(Long bookingId) {
            this.bookingId = bookingId;
        }

        public PaymentMethod getMethod() {
            return method;
        }

        public void setMethod(PaymentMethod method) {
            this.method = method;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}

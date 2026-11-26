package com.absolutecinema.payment;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.absolutecinema.auth.User;
import com.absolutecinema.auth.UserService;
import com.absolutecinema.booking.BookingService;
import com.absolutecinema.booking.BookingStatus;

@RestController
@RequestMapping("/bookings/{id}/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final UserService userService;

    public PaymentController(PaymentService paymentService,
                             BookingService bookingService,
                             UserService userService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> pay(@PathVariable Long id,
                                 @RequestBody PaymentRequest request,
                                 Principal principal) {
        User user = userService.getByEmail(principal.getName());
        // Si la reserva no es del usuario autenticado, lanza 404
        bookingService.getBooking(id, user);

        try {
            Payment payment = paymentService.pay(id, request);
            return ResponseEntity.ok(PaymentResponse.from(payment));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "REJECTED", "message", e.getMessage()));

        } catch (IllegalStateException e) {
            boolean paid = bookingService.getBooking(id, user).getStatus() == BookingStatus.CONFIRMED;
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("code", paid ? "PAID" : "EXPIRED", "message", e.getMessage()));
        }
    }
}
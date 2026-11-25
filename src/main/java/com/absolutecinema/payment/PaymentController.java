package com.absolutecinema.payment;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.absolutecinema.auth.User;
import com.absolutecinema.auth.UserService;
import com.absolutecinema.booking.Booking;
import com.absolutecinema.booking.BookingService;
import com.absolutecinema.booking.BookingStatus;

@Controller
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

    @GetMapping
    public String show(@PathVariable Long id, Principal principal,
                       Model model, RedirectAttributes redirect) {
        User user = userService.getByEmail(principal.getName());
        Booking booking = bookingService.getBooking(id, user);   // 404 si no es suya

        String redirectTo = redirectIfNotPayable(booking, redirect);
        if (redirectTo != null) {
            return redirectTo;
        }

        fillModel(model, booking);
        model.addAttribute("paymentRequest", new PaymentRequest());
        return "payment/payment";
    }

    @PostMapping
    public String pay(@PathVariable Long id,
                      @ModelAttribute("paymentRequest") PaymentRequest request,
                      Principal principal, Model model, RedirectAttributes redirect) {
        User user = userService.getByEmail(principal.getName());
        Booking booking = bookingService.getBooking(id, user);   // 404 si no es suya

        try {
            Payment payment = paymentService.pay(id, request);

            if (payment.getStatus() == PaymentStatus.APPROVED) {
                return "redirect:/bookings/" + id + "/confirmation";
            }

            // Rechazado: se vuelve a mostrar el formulario con el motivo y se permite reintentar
            fillModel(model, booking);
            model.addAttribute("rejection", payment.getRejectionReason());
            model.addAttribute("selectedMethod", request.getMethod());
            clearSensitiveData(request);
            return "payment/payment";

        } catch (IllegalStateException e) {
            // Vencida, ya pagada o ya no pendiente
            Booking fresh = bookingService.getBooking(id, user);
            if (fresh.getStatus() == BookingStatus.CONFIRMED) {
                return "redirect:/bookings/" + id + "/confirmation";
            }
            redirect.addFlashAttribute("notice",
                    "Tu reserva venció. Elige tus asientos nuevamente.");
            return "redirect:/";

        } catch (IllegalArgumentException e) {
            fillModel(model, booking);
            model.addAttribute("rejection", e.getMessage());
            clearSensitiveData(request);
            return "payment/payment";
        }
    }

    // ---------------------------------------------------------------

    /** null si la reserva se puede pagar; si no, la redirección que corresponde. */
    private String redirectIfNotPayable(Booking booking, RedirectAttributes redirect) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return "redirect:/bookings/" + booking.getId() + "/confirmation";
        }
        boolean expired = booking.getStatus() != BookingStatus.PENDING
                || !booking.getExpiresAt().isAfter(LocalDateTime.now());
        if (expired) {
            redirect.addFlashAttribute("notice",
                    "Tu reserva venció. Elige tus asientos nuevamente.");
            return "redirect:/";
        }
        return null;
    }

    private void fillModel(Model model, Booking booking) {
        long secondsLeft = Math.max(0,
                Duration.between(LocalDateTime.now(), booking.getExpiresAt()).getSeconds());

        model.addAttribute("booking", booking);                 // booking.expiresAt, booking.total, booking.code
        model.addAttribute("showtime", booking.getShowtime());  // showtime.movie, showtime.startsAt, showtime.room
        model.addAttribute("seats", bookingService.getSeatLabels(booking.getId()));
        model.addAttribute("secondsLeft", secondsLeft);         // para el contador (evita depender del reloj del navegador)
    }

    /** Nunca se reenvían al navegador el número de tarjeta, el CVV ni el DNI. */
    private void clearSensitiveData(PaymentRequest request) {
        request.setCardNumber(null);
        request.setCvv(null);
        request.setDni(null);
    }
}

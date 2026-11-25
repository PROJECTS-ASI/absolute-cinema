package com.absolutecinema.booking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.absolutecinema.auth.User;
import com.absolutecinema.auth.UserService;
import com.absolutecinema.catalog.Showtime;
import com.absolutecinema.payment.Payment;
import com.absolutecinema.payment.PaymentRepository;
import com.absolutecinema.payment.PaymentStatus;

@Controller
public class ConfirmationController {

    private static final Locale LOCALE = Locale.forLanguageTag("es-PE");

    public record Ticket(String seatLabel, String priceLabel) { }

    private final BookingService bookingService;
    private final UserService userService;
    private final PaymentRepository paymentRepository;

    public ConfirmationController(BookingService bookingService, UserService userService,
                                  PaymentRepository paymentRepository) {
        this.bookingService = bookingService;
        this.userService = userService;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/bookings/{id}/confirmation")
    public String confirmation(@PathVariable Long id, Principal principal, Model model) {
        User user = userService.getByEmail(principal.getName());
        Booking booking = bookingService.getBooking(id, user);       // 404 if it is not the user's

        if (booking.getStatus() == BookingStatus.PENDING) {
            return "redirect:/bookings/" + id + "/payment";          // not paid yet
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return "redirect:/";                                      // expired or incomplete
        }

        Payment payment = paymentRepository.findByBookingId(id).stream()
                .filter(p -> p.getStatus() == PaymentStatus.APPROVED)
                .findFirst().orElse(null);

        Showtime showtime = booking.getShowtime();
        String priceLabel = money(showtime.getPrice());
        List<String> seats = bookingService.getSeatLabels(id);

        model.addAttribute("booking", booking);
        model.addAttribute("userName", user.getName());
        model.addAttribute("dateLabel", capitalize(showtime.getStartsAt()
                .format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", LOCALE))));
        model.addAttribute("timeLabel", formatTime(showtime.getStartsAt().toLocalTime()));
        model.addAttribute("tickets", seats.stream().map(s -> new Ticket(s, priceLabel)).toList());
        model.addAttribute("ticketCount", seats.size());
        model.addAttribute("totalLabel", money(booking.getTotal()));
        model.addAttribute("methodLabel", methodLabel(payment));
        return "booking/confirmation";
    }

    private static String methodLabel(Payment payment) {
        if (payment == null) {
            return "Pago simulado";
        }
        return switch (payment.getMethod()) {
            case MANUAL_AMOUNT -> "Importe manual";
            case CARD -> payment.getCardBrand() == null
                    ? "Tarjeta simulada" : "Tarjeta simulada · " + payment.getCardBrand();
        };
    }

    private static String money(BigDecimal value) {
        return "S/ " + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatTime(LocalTime t) {
        int hour = t.getHour() % 12 == 0 ? 12 : t.getHour() % 12;
        return String.format("%d:%02d %s", hour, t.getMinute(), t.getHour() < 12 ? "a. m." : "p. m.");
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(LOCALE) + s.substring(1);
    }
}
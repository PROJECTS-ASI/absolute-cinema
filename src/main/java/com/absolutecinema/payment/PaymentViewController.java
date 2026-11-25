package com.absolutecinema.payment;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.absolutecinema.auth.User;
import com.absolutecinema.auth.UserService;
import com.absolutecinema.booking.Booking;
import com.absolutecinema.booking.BookingService;
import com.absolutecinema.booking.BookingStatus;

@Controller
public class PaymentViewController {
	private final BookingService bookingService;
    private final UserService userService;

    public PaymentViewController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
}
    @GetMapping("/bookings/{id}/payment")
    public String paymentPage(@PathVariable Long id, Principal principal, Model model) {
        User user = userService.getByEmail(principal.getName());
        Booking booking = bookingService.getBooking(id, user);

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return "redirect:/bookings/" + id + "/confirmation";
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            return "redirect:/";
        }

        List<String> seatLabels = bookingService.getSeatLabels(id);

        model.addAttribute("booking", booking);
        model.addAttribute("seatLabels", seatLabels);
        model.addAttribute("ticketCount", seatLabels.size());
        return "payment/payment";
    }
}

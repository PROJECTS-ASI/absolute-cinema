package com.absolutecinema.booking;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.absolutecinema.auth.User;
import com.absolutecinema.auth.UserService;
import com.absolutecinema.catalog.Showtime;

@Controller
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    public BookingController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @GetMapping("/showtimes/{id}/seats")
    public String seats(@PathVariable Long id, Principal principal, Model model) {
        User user = userService.getByEmail(principal.getName());
        Showtime showtime = bookingService.getShowtime(id);

        // Coming back to the map drops the user's previous pending selection
        bookingService.cancelPending(id, user);

        model.addAttribute("showtime", showtime);
        model.addAttribute("rows", toRows(bookingService.getSeatMap(id)));
        model.addAttribute("maxSeats", BookingService.MAX_SEATS_PER_BOOKING);
        // "error" and "unavailable" arrive as flash attributes after a failed lock
        return "booking/seats";
    }

    @PostMapping("/showtimes/{id}/seats")
    public String lock(@PathVariable Long id,
                       @RequestParam(name = "seatIds", required = false) List<Long> seatIds,
                       Principal principal,
                       RedirectAttributes redirect) {
        User user = userService.getByEmail(principal.getName());
        try {
            Booking booking = bookingService.lockSeats(id, seatIds, user);
            return "redirect:/bookings/" + booking.getId() + "/payment";
        } catch (SeatsUnavailableException e) {
            redirect.addFlashAttribute("unavailable", e.getSeatLabels());
            redirect.addFlashAttribute("error", "Estos asientos ya no están disponibles: "
                    + String.join(", ", e.getSeatLabels()) + ". Elige otros.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/showtimes/" + id + "/seats";
    }

    private static List<SeatRow> toRows(List<SeatView> seats) {
        Map<String, List<SeatView>> byRow = new LinkedHashMap<>();
        for (SeatView seat : seats) {
            byRow.computeIfAbsent(seat.rowLabel(), k -> new ArrayList<>()).add(seat);
        }
        return byRow.entrySet().stream()
                .map(e -> new SeatRow(e.getKey(), e.getValue()))
                .toList();
    }
}
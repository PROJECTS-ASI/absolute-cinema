package com.absolutecinema.catalog;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CatalogController {

    private static final Locale LOCALE = Locale.forLanguageTag("es-PE");

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("movies", catalogService.getNowShowing());
        model.addAttribute("minPriceLabel", catalogService.getMinPrice()
                .map(p -> "S/ " + p.setScale(2, RoundingMode.HALF_UP).toPlainString())
                .orElse(null));
        model.addAttribute("weekLabel",
                LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM", LOCALE)));
        return "catalog/home";
    }

    /** Movie detail + showtimes of one day (?date=yyyy-MM-dd, default: first day with showtimes). */
    @GetMapping("/movies/{id}/showtimes")
    public String showtimes(@PathVariable Long id,
                            @RequestParam(required = false) LocalDate date,
                            Model model) {
        List<LocalDate> dates = catalogService.getAvailableDates(id);
        LocalDate selected = date != null ? date : (dates.isEmpty() ? LocalDate.now() : dates.get(0));
        DateTimeFormatter tab = DateTimeFormatter.ofPattern("EEE d MMM", LOCALE);

        model.addAttribute("movie", catalogService.getMovie(id));
        model.addAttribute("dates", dates.stream()
                .map(d -> new DateTab(d, d.format(tab), d.equals(selected))).toList());
        model.addAttribute("showtimes", catalogService.getShowtimes(id, selected).stream()
                .map(s -> new ShowtimeCard(s.getId(),
                        s.getStartsAt().format(DateTimeFormatter.ofPattern("HH:mm")),
                        s.getRoom().getName(),
                        "S/ " + s.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString()))
                .toList());
        return "catalog/showtimes";
    }

    public record DateTab(LocalDate date, String label, boolean active) { }

    public record ShowtimeCard(Long id, String time, String room, String price) { }
}
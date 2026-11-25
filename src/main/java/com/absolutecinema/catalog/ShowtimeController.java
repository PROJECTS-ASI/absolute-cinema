package com.absolutecinema.catalog;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ShowtimeController {

    private static final Locale LOCALE = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("EEE", LOCALE);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", LOCALE);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** One tab of the date selector. */
    public record DayTab(String iso, String weekday, String day, String month, boolean selected) { }

    /** One showtime card, already formatted for the view. */
    public record ShowtimeOption(Long id, String time, String room, String price) { }

    private final CatalogService catalogService;

    public ShowtimeController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /** Showtimes of a movie. Without ?date=, shows the first day that still has showtimes. */
    @GetMapping("/movies/{id}/showtimes")
    public String showtimes(@PathVariable Long id,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            Model model) {
        Movie movie = catalogService.getMovie(id);
        List<LocalDate> dates = catalogService.getShowtimeDates(id);

        LocalDate today = LocalDate.now();
        LocalDate selected = date != null ? date : (dates.isEmpty() ? today : dates.get(0));

        List<DayTab> tabs = dates.stream()
                .map(d -> new DayTab(
                        d.toString(),
                        d.equals(today) ? "Hoy" : d.format(WEEKDAY),
                        String.valueOf(d.getDayOfMonth()),
                        d.format(MONTH),
                        d.equals(selected)))
                .toList();

        List<ShowtimeOption> options = catalogService.getShowtimes(id, selected).stream()
                .map(s -> new ShowtimeOption(
                        s.getId(),
                        s.getStartsAt().format(TIME),
                        s.getRoom().getName(),
                        "S/ " + s.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString()))
                .toList();

        model.addAttribute("movie", movie);
        model.addAttribute("tabs", tabs);
        model.addAttribute("selectedLabel", selected.format(LONG_DATE));
        model.addAttribute("showtimes", options);
        return "catalog/movie-showtimes";
    }
}

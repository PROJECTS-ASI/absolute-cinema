package com.absolutecinema.catalog;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
    
    @GetMapping("/movies/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("movie", catalogService.getMovie(id));
        model.addAttribute("hasShowtimes", !catalogService.getShowtimeDates(id).isEmpty());
        return "catalog/movie-detail";
    }
}
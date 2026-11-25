
package com.absolutecinema.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.server.ResponseStatusException;

@Controller
public class ShowtimeController {

    private final CatalogService catalogService;

    public ShowtimeController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/movies/{id}/showtimes")
    public String showtimes(
            @PathVariable Long id,
            Model model) {

        Movie movie = catalogService.getMovieById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Película no encontrada"));

        model.addAttribute("movie", movie);
        model.addAttribute(
                "showtimes",
                catalogService.getUpcomingShowtimes(id));

        return "Catalog/showtimes";
    }
}


package com.absolutecinema.booking;

import com.absolutecinema.catalog.CatalogService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class BookingPreviewController {

    private final CatalogService catalogService;

    public BookingPreviewController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/movies/{id}/showtimes")
    public String mostrarPantallaReservas(
            @PathVariable Long id,
            Model model) {

        var movie = catalogService.getMovieById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Película no encontrada"));

        model.addAttribute("movie", movie);

        return "Booking/seat-selection";
    }

    @GetMapping("/reservas/prueba")
    public String mostrarPrueba(Model model) {
        return "Booking/seat-selection";
    }
}

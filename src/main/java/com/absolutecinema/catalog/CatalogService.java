package com.absolutecinema.catalog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    public CatalogService(MovieRepository movieRepository, ShowtimeRepository showtimeRepository) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
    }

    /** Active movies that still have upcoming showtimes. */
    public List<Movie> getNowShowing() {
        return movieRepository.findNowShowing(LocalDateTime.now());
    }

    /** Lowest ticket price among upcoming showtimes. */
    public Optional<BigDecimal> getMinPrice() {
        return showtimeRepository.findMinPriceFrom(LocalDateTime.now());
    }

    /** Active movie by id; 404 if it does not exist or is not active. */
    public Movie getMovie(Long id) {
        return movieRepository.findById(id)
                .filter(Movie::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Película no encontrada"));
    }

    /** Days (ascending, no repeats) on which the movie still has upcoming showtimes. */
    public List<LocalDate> getShowtimeDates(Long movieId) {
        return showtimeRepository
                .findByMovieIdAndStartsAtAfterOrderByStartsAt(movieId, LocalDateTime.now())
                .stream()
                .map(s -> s.getStartsAt().toLocalDate())
                .distinct()
                .toList();
    }

    /** Showtimes of a movie on a given day, ordered by time. Past showtimes of today are left out. */
    public List<Showtime> getShowtimes(Long movieId, LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = date.atStartOfDay();
        if (from.isBefore(now)) {
            from = now;
        }
        LocalDateTime to = date.plusDays(1).atStartOfDay().minusNanos(1);
        return showtimeRepository.findByMovieIdAndStartsAtBetweenOrderByStartsAt(movieId, from, to);
    }
}
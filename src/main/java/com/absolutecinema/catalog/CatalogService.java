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

    private static final int DAYS_AHEAD = 7;

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

    /** Active movie or 404. */
    public Movie getMovie(Long id) {
        return movieRepository.findById(id)
                .filter(Movie::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Película no encontrada"));
    }

    /** Days (today + next 6) that have at least one upcoming showtime for the movie. */
    public List<LocalDate> getAvailableDates(Long movieId) {
        LocalDateTime now = LocalDateTime.now();
        return showtimeRepository
                .findByMovieIdAndStartsAtBetweenOrderByStartsAt(
                        movieId, now, now.toLocalDate().plusDays(DAYS_AHEAD).atStartOfDay())
                .stream()
                .map(s -> s.getStartsAt().toLocalDate())
                .distinct()
                .toList();
    }

    /** Upcoming showtimes of the movie on that day (already started ones are hidden). */
    public List<Showtime> getShowtimes(Long movieId, LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = date.atStartOfDay().isBefore(now) ? now : date.atStartOfDay();
        return showtimeRepository.findByMovieIdAndStartsAtBetweenOrderByStartsAt(
                movieId, from, date.plusDays(1).atStartOfDay());
    }
}
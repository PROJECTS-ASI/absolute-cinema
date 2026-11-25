package com.absolutecinema.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    public Optional<Movie> getMovieById(Long id) {
        return movieRepository.findById(id);
    }

    /** Lowest ticket price among upcoming showtimes. */
    public Optional<BigDecimal> getMinPrice() {
        return showtimeRepository.findMinPriceFrom(LocalDateTime.now());
    }
}
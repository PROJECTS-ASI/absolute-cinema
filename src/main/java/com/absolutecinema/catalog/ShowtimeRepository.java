package com.absolutecinema.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByMovieIdAndStartsAtBetweenOrderByStartsAt(Long movieId, LocalDateTime from, LocalDateTime to);
    boolean existsByRoomIdAndStartsAt(Long roomId, LocalDateTime startsAt);
    
    @Query("select min(s.price) from Showtime s where s.startsAt > :now")
    Optional<BigDecimal> findMinPriceFrom(@Param("now") LocalDateTime now);
    
    List<Showtime> findByMovieIdAndStartsAtAfterOrderByStartsAt(
            Long movieId, LocalDateTime from);
}
package com.absolutecinema.catalog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByActiveTrueOrderByTitle();    
    boolean existsByTitle(String title);
    Optional<Movie> findByTitle(String title);
    
    @Query("select m from Movie m where m.active = true and exists "
            + "(select 1 from Showtime s where s.movie = m and s.startsAt > :now) "
            + "order by m.title")
    List<Movie> findNowShowing(@Param("now") LocalDateTime now);
}
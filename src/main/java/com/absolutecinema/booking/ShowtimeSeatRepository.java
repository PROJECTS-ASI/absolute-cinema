package com.absolutecinema.booking;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.absolutecinema.catalog.Showtime;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {
    List<ShowtimeSeat> findByShowtimeId(Long showtimeId);
    List<ShowtimeSeat> findByBookingId(Long bookingId);
    long countByShowtimeIdAndStatus(Long showtimeId, SeatStatus status);
    
    @Query("select s from Showtime s where not exists " + "(select 1 from ShowtimeSeat ss where ss.showtime = s)")
    List<Showtime> findShowtimesWithoutSeats();
}
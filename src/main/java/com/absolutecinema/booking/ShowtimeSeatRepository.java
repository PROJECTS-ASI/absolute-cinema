package com.absolutecinema.booking;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.absolutecinema.catalog.Showtime;

import jakarta.persistence.LockModeType;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {
    List<ShowtimeSeat> findByShowtimeId(Long showtimeId);
    List<ShowtimeSeat> findByBookingId(Long bookingId);
    long countByShowtimeIdAndStatus(Long showtimeId, SeatStatus status);
    
    @Query("select s from Showtime s where not exists " + "(select 1 from ShowtimeSeat ss where ss.showtime = s)")
    List<Showtime> findShowtimesWithoutSeats();
    
    @Query("select ss from ShowtimeSeat ss join fetch ss.seat s "
        + "where ss.showtime.id = :showtimeId order by s.rowLabel, s.seatNumber")
   List<ShowtimeSeat> findMap(@Param("showtimeId") Long showtimeId);

   // SELECT ... FOR UPDATE, always ordered by seat id (avoids deadlocks)
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("select ss from ShowtimeSeat ss where ss.showtime.id = :showtimeId "
        + "and ss.seat.id in :seatIds order by ss.seat.id")
   List<ShowtimeSeat> findForUpdate(@Param("showtimeId") Long showtimeId,
                                    @Param("seatIds") Collection<Long> seatIds);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("select ss from ShowtimeSeat ss where ss.booking.id = :bookingId order by ss.seat.id")
   List<ShowtimeSeat> findByBookingIdForUpdate(@Param("bookingId") Long bookingId);

   @Modifying(flushAutomatically = true)
   @Query("update ShowtimeSeat ss set ss.status = com.absolutecinema.booking.SeatStatus.AVAILABLE, "
        + "ss.lockedBy = null, ss.lockExpiresAt = null, ss.booking = null "
        + "where ss.booking.id = :bookingId "
        + "and ss.status = com.absolutecinema.booking.SeatStatus.LOCKED")
   int releaseByBooking(@Param("bookingId") Long bookingId);

   @Modifying(flushAutomatically = true, clearAutomatically = true)
   @Query("update ShowtimeSeat ss set ss.status = com.absolutecinema.booking.SeatStatus.AVAILABLE, "
        + "ss.lockedBy = null, ss.lockExpiresAt = null, ss.booking = null "
        + "where ss.status = com.absolutecinema.booking.SeatStatus.LOCKED "
        + "and ss.lockExpiresAt < :now")
   int releaseExpired(@Param("now") LocalDateTime now);
}
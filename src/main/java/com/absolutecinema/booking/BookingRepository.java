package com.absolutecinema.booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.absolutecinema.auth.User;

import jakarta.persistence.LockModeType;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    List<Booking> findByUserIdAndShowtimeIdAndStatus(Long userId, Long showtimeId, BookingStatus status);

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Booking b set b.status = com.absolutecinema.booking.BookingStatus.EXPIRED "
         + "where b.status = com.absolutecinema.booking.BookingStatus.PENDING and b.expiresAt < :now")
    int expirePending(@Param("now") LocalDateTime now);
}
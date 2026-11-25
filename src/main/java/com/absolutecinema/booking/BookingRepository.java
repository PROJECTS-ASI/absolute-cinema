package com.absolutecinema.booking;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.absolutecinema.auth.User;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserOrderByCreatedAtDesc(User user);
}
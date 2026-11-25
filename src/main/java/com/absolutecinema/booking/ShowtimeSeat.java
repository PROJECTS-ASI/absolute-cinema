package com.absolutecinema.booking;

import java.time.LocalDateTime;

import com.absolutecinema.auth.User;
import com.absolutecinema.catalog.Seat;
import com.absolutecinema.catalog.Showtime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "showtime_seats", uniqueConstraints = @UniqueConstraint(columnNames = {"showtime_id", "seat_id"}))
public class ShowtimeSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Showtime showtime;

    @ManyToOne(optional = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @ManyToOne
    private User lockedBy;

    private LocalDateTime lockExpiresAt;

    @ManyToOne
    private Booking booking;      // booking that locked or bought the seat

    @Version
    private Long version;         // optimistic locking, extra protection

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Showtime getShowtime() {
		return showtime;
	}

	public void setShowtime(Showtime showtime) {
		this.showtime = showtime;
	}

	public Seat getSeat() {
		return seat;
	}

	public void setSeat(Seat seat) {
		this.seat = seat;
	}

	public SeatStatus getStatus() {
		return status;
	}

	public void setStatus(SeatStatus status) {
		this.status = status;
	}

	public User getLockedBy() {
		return lockedBy;
	}

	public void setLockedBy(User lockedBy) {
		this.lockedBy = lockedBy;
	}

	public LocalDateTime getLockExpiresAt() {
		return lockExpiresAt;
	}

	public void setLockExpiresAt(LocalDateTime lockExpiresAt) {
		this.lockExpiresAt = lockExpiresAt;
	}

	public Booking getBooking() {
		return booking;
	}

	public void setBooking(Booking booking) {
		this.booking = booking;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}
}
package com.absolutecinema.booking;

import java.util.List;
import com.absolutecinema.auth.User;
import com.absolutecinema.catalog.Showtime;

public interface BookingService {

    /** State of every seat of a showtime. Locks that already expired count as AVAILABLE. */
    List<SeatView> getSeatMap(Long showtimeId);

    /**
     * All or nothing: locks every seat for 5 minutes, or none.
     * Throws SeatsUnavailableException (with the taken seats) if any seat is not free.
     */
    Booking lockSeats(Long showtimeId, List<Long> seatIds, User user);

    /** Returns the booking if it belongs to the user; throws otherwise. */
    Booking getBooking(Long bookingId, User user);

    /** Called by the payment module, inside its own transaction, after an approved payment. */
    Booking confirm(Long bookingId);

    /** Bookings of the user, newest first. */
    List<Booking> getUserBookings(User user);
    
    int MAX_SEATS_PER_BOOKING = 8;

    /** Showtime for the seat map screen (404 if it does not exist). */
    Showtime getShowtime(Long showtimeId);

    /** Drops the user's pending booking for that showtime and frees its seats. */
    void cancelPending(Long showtimeId, User user);

    /** Seat labels of a booking, e.g. ["E7", "E8"]. */
    List<String> getSeatLabels(Long bookingId);

    /** Frees expired locks and marks expired bookings. Returns how many seats were freed. */
    int releaseExpired();
}
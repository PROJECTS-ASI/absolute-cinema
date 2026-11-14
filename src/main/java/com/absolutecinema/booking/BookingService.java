package com.absolutecinema.booking;

import java.util.List;
import com.absolutecinema.auth.User;

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
}
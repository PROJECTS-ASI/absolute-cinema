package com.absolutecinema.booking;

import java.util.List;
import org.springframework.stereotype.Service;
import com.absolutecinema.auth.User;

@Service
public class BookingServiceImpl implements BookingService {

    @Override
    public List<SeatView> getSeatMap(Long showtimeId) {
        throw new UnsupportedOperationException("TODO: seats module owner");
    }

    @Override
    public Booking lockSeats(Long showtimeId, List<Long> seatIds, User user) {
        throw new UnsupportedOperationException("TODO: seats module owner");
    }

    @Override
    public Booking getBooking(Long bookingId, User user) {
        throw new UnsupportedOperationException("TODO: seats module owner");
    }

    @Override
    public Booking confirm(Long bookingId) {
        throw new UnsupportedOperationException("TODO: seats module owner");
    }

    @Override
    public List<Booking> getUserBookings(User user) {
        throw new UnsupportedOperationException("TODO: seats module owner");
    }
}
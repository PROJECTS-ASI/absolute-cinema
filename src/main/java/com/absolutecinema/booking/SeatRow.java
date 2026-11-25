package com.absolutecinema.booking;

import java.util.List;

/** One row of the seat map (A, B, ...) with its seats, ready for the view. */
public record SeatRow(String label, List<SeatView> seats) { }
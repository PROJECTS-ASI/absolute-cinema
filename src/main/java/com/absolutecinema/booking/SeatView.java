package com.absolutecinema.booking;

public record SeatView(Long seatId, String rowLabel, int seatNumber, SeatStatus status) { }
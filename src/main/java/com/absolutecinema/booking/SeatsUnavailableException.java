package com.absolutecinema.booking;

import java.util.List;

public class SeatsUnavailableException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final List<String> seatLabels;   // e.g. ["A2", "A3"]

    public SeatsUnavailableException(List<String> seatLabels) {
        super("Seats no longer available: " + seatLabels);
        this.seatLabels = seatLabels;
    }

    public List<String> getSeatLabels() {
        return seatLabels;
    }
}
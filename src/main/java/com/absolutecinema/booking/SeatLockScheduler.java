package com.absolutecinema.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SeatLockScheduler {

    private static final Logger log = LoggerFactory.getLogger(SeatLockScheduler.class);

    private final BookingService bookingService;

    public SeatLockScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelay = 30_000)
    public void releaseExpiredLocks() {
        int released = bookingService.releaseExpired();
        if (released > 0) {
            log.info("Released {} expired seat locks", released);
        }
    }
}
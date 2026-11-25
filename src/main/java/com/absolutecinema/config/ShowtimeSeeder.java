package com.absolutecinema.config;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.absolutecinema.booking.SeatStatus;
import com.absolutecinema.booking.ShowtimeSeat;
import com.absolutecinema.booking.ShowtimeSeatRepository;
import com.absolutecinema.catalog.Movie;
import com.absolutecinema.catalog.MovieRepository;
import com.absolutecinema.catalog.Room;
import com.absolutecinema.catalog.RoomRepository;
import com.absolutecinema.catalog.Seat;
import com.absolutecinema.catalog.SeatRepository;
import com.absolutecinema.catalog.Showtime;
import com.absolutecinema.catalog.ShowtimeRepository;

import jakarta.transaction.Transactional;

@Component
@Order(2)
public class ShowtimeSeeder implements CommandLineRunner {

    private static final int DAYS_AHEAD = 7;
    private static final BigDecimal PRICE = new BigDecimal("9.00");
    // Demo only: about 1 of every 8 seats starts as SOLD so the map looks realistic.
    // Set to 0 to disable.
    private static final int DEMO_SOLD_MODULO = 8;

    private static final Set<DayOfWeek> ALL_DAYS = EnumSet.allOf(DayOfWeek.class);
    private static final Set<DayOfWeek> WEEKEND =
            EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private record Slot(String movieTitle, String roomName, List<String> times, Set<DayOfWeek> days) { }

    // ===== EDIT HERE: schedule (movie, room, start times, days of the week) =====
    private static final List<Slot> SCHEDULE = List.of(
        new Slot("El último horizonte",   "Sala 01", List.of("17:15", "20:30", "22:45"), ALL_DAYS),
        new Slot("Después de medianoche", "Sala 02", List.of("16:30", "19:15", "21:45"), ALL_DAYS),
        new Slot("Donde empieza el mar",  "Sala 03", List.of("15:45", "18:30", "21:00"), ALL_DAYS),
        new Slot("El eco del bosque",     "Sala 04", List.of("17:00", "19:45", "22:15"), WEEKEND)
    );

    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;

    public ShowtimeSeeder(MovieRepository movieRepository, RoomRepository roomRepository,
                          SeatRepository seatRepository, ShowtimeRepository showtimeRepository,
                          ShowtimeSeatRepository showtimeSeatRepository) {
        this.movieRepository = movieRepository;
        this.roomRepository = roomRepository;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        LocalDate today = LocalDate.now();
        for (int d = 0; d < DAYS_AHEAD; d++) {
            LocalDate day = today.plusDays(d);
            for (Slot slot : SCHEDULE) {
                if (!slot.days().contains(day.getDayOfWeek())) {
                    continue;
                }
                Movie movie = movieRepository.findByTitle(slot.movieTitle()).orElse(null);
                Room room = roomRepository.findByName(slot.roomName()).orElse(null);
                if (movie == null || room == null) {
                    continue;
                }
                for (String time : slot.times()) {
                    LocalDateTime startsAt = LocalDateTime.of(day, LocalTime.parse(time));
                    if (!showtimeRepository.existsByRoomIdAndStartsAt(room.getId(), startsAt)) {
                        Showtime st = new Showtime();
                        st.setMovie(movie);
                        st.setRoom(room);
                        st.setStartsAt(startsAt);
                        st.setPrice(PRICE);
                        showtimeRepository.save(st);
                    }
                }
            }
        }
        fillMissingSeats();
    }

    // Creates the seat rows of every showtime that has none (also covers showtimes
    // inserted by hand from pgAdmin).
    private void fillMissingSeats() {
        for (Showtime st : showtimeSeatRepository.findShowtimesWithoutSeats()) {
            List<ShowtimeSeat> rows = new ArrayList<>();
            for (Seat seat : seatRepository.findByRoom(st.getRoom())) {
                ShowtimeSeat ss = new ShowtimeSeat();
                ss.setShowtime(st);
                ss.setSeat(seat);
                boolean sold = DEMO_SOLD_MODULO > 0
                        && (seat.getId() + st.getId()) % DEMO_SOLD_MODULO == 0;
                ss.setStatus(sold ? SeatStatus.SOLD : SeatStatus.AVAILABLE);
                rows.add(ss);
            }
            showtimeSeatRepository.saveAll(rows);
        }
    }
}
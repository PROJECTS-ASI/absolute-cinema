package com.absolutecinema.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.absolutecinema.catalog.Movie;
import com.absolutecinema.catalog.MovieRepository;
import com.absolutecinema.catalog.Room;
import com.absolutecinema.catalog.RoomRepository;
import com.absolutecinema.catalog.Seat;
import com.absolutecinema.catalog.SeatRepository;

import jakarta.transaction.Transactional;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final int ROWS = 8;
    private static final int COLUMNS = 12;
    private static final String[] ROOMS = {"Sala 01", "Sala 02", "Sala 03", "Sala 04"};

    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;

    public DataSeeder(MovieRepository movieRepository, RoomRepository roomRepository,
                      SeatRepository seatRepository) {
        this.movieRepository = movieRepository;
        this.roomRepository = roomRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedMovies();
        seedRooms();
    }

    private void seedMovies() {
        movie("El último horizonte",
              "Una astronauta recibe una señal desde el límite del universo. "
              + "Para volver a casa, tendrá que descubrir qué queda al otro lado del silencio.",
              118, "+12", "Ciencia ficción · Aventura", "Clara Vidal", "Elena Sol, Marcos Rey",
              "/img/posters/ultimo-horizonte.png");
        movie("Después de medianoche",
              "Una noche, una ciudad y un secreto que nadie quiere contar.",
              106, "+16", "Thriller · Suspense", "Por definir", "Por definir",
              "/img/posters/despues-de-medianoche.png");
        movie("Donde empieza el mar",
              "Dos desconocidos se cruzan en un acantilado y nada vuelve a ser igual.",
              102, "+7", "Drama · Romance", "Por definir", "Por definir",
              "/img/posters/donde-empieza-el-mar.png");
        movie("El eco del bosque",
              "Una cabaña aislada guarda algo más que silencio.",
              112, "+12", "Misterio · Drama", "Por definir", "Por definir",
              "/img/posters/eco-del-bosque.png");
    }

    private void movie(String title, String synopsis, int minutes, String rating,
                       String genre, String director, String castMembers, String posterUrl) {
        if (movieRepository.existsByTitle(title)) {
            return;
        }
        Movie m = new Movie();
        m.setTitle(title);
        m.setSynopsis(synopsis);
        m.setDurationMinutes(minutes);
        m.setRating(rating);
        m.setGenre(genre);
        m.setDirector(director);
        m.setCastMembers(castMembers);
        m.setPosterUrl(posterUrl);
        m.setActive(true);
        movieRepository.save(m);
    }

    private void seedRooms() {
        for (String name : ROOMS) {
            if (roomRepository.findByName(name).isPresent()) {
                continue;
            }
            Room room = new Room();
            room.setName(name);
            room.setRowCount(ROWS);
            room.setColumnCount(COLUMNS);
            room = roomRepository.save(room);

            List<Seat> seats = new ArrayList<>();
            for (int r = 0; r < ROWS; r++) {
                String label = String.valueOf((char) ('A' + r));
                for (int n = 1; n <= COLUMNS; n++) {
                    Seat seat = new Seat();
                    seat.setRoom(room);
                    seat.setRowLabel(label);
                    seat.setSeatNumber(n);
                    seats.add(seat);
                }
            }
            seatRepository.saveAll(seats);
        }
    }
}
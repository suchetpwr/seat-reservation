package com.suchet.seat_reservation.service;

import com.suchet.seat_reservation.dto.CreateShowRequest;
import com.suchet.seat_reservation.dto.ShowResponse;
import com.suchet.seat_reservation.model.Seat;
import com.suchet.seat_reservation.model.Show;
import com.suchet.seat_reservation.repository.SeatRepository;
import com.suchet.seat_reservation.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.suchet.seat_reservation.dto.ShowDetailsResponse;
import com.suchet.seat_reservation.exception.ShowNotFoundException;
import com.suchet.seat_reservation.model.SeatStatus;
import com.suchet.seat_reservation.model.Seat;
import java.util.UUID;

import java.util.List;
import java.util.UUID;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    public ShowService(
            ShowRepository showRepository,
            SeatRepository seatRepository
    ) {
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public ShowResponse createShow(CreateShowRequest request) {

        UUID showId = UUID.randomUUID();

        Show show = new Show(
                showId,
                request.name(),
                request.pricePaise(),
                4
        );

        showRepository.save(show);

        List<Seat> seats = request.seats()
                .stream()
                .map(seatNumber ->
                        new Seat(
                                UUID.randomUUID(),
                                showId,
                                seatNumber
                        )
                )
                .toList();

        seatRepository.saveAll(seats);

        return new ShowResponse(
                showId,
                show.getName(),
                show.getPricePaise(),
                request.seats()
        );
    }

    @Transactional(readOnly = true)
    public ShowDetailsResponse getShow(UUID showId) {

        Show show = showRepository.findById(showId)
                .orElseThrow(ShowNotFoundException::new);

        var seats = seatRepository.findByShowId(showId);

        int totalSeats = seats.size();

        int availableSeats = (int) seats.stream()
                .filter(seat ->
                        seat.getStatus() == SeatStatus.AVAILABLE
                )
                .count();

        int heldSeats = (int) seats.stream()
                .filter(seat ->
                        seat.getStatus() == SeatStatus.HELD
                )
                .count();

        int confirmedSeats = (int) seats.stream()
                .filter(seat ->
                        seat.getStatus() == SeatStatus.CONFIRMED
                )
                .count();

        var seatDetails = seats.stream()
                .sorted(java.util.Comparator.comparing(Seat::getSeatNumber))
                .map(seat ->
                        new ShowDetailsResponse.SeatDetails(
                                seat.getSeatNumber(),
                                seat.getStatus().name()
                        )
                )
                .toList();

        return new ShowDetailsResponse(
                show.getId().toString(),
                show.getName(),
                show.getPricePaise(),
                totalSeats,
                availableSeats,
                heldSeats,
                confirmedSeats,
                seatDetails
        );
    }
}
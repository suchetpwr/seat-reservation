package com.suchet.seat_reservation.dto;

import java.util.List;

public record ShowDetailsResponse(
        String id,
        String name,
        long pricePaise,
        int totalSeats,
        int availableSeats,
        int heldSeats,
        int confirmedSeats,
        List<SeatDetails> seats
) {

    public record SeatDetails(
            String seatNumber,
            String status
    ) {}
}
package com.suchet.seat_reservation.dto;

import java.util.List;
import java.util.UUID;

public record ShowResponse(
        UUID id,
        String name,
        long pricePaise,
        List<String> seats
) {
}
package com.suchet.seat_reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record CreateShowRequest(

        @NotBlank
        String name,

        @NotEmpty
        List<@NotBlank String> seats,

        @PositiveOrZero
        long pricePaise

) {
}
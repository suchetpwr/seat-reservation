package com.suchet.seat_reservation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReserveRequest(

        @NotEmpty(message = "seats must not be empty")
        @Size(max = 4, message = "maximum 4 seats per reservation")
        List<String> seats

) {
}
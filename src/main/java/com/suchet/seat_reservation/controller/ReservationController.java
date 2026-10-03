package com.suchet.seat_reservation.controller;

import com.suchet.seat_reservation.dto.ReservationResponse;
import com.suchet.seat_reservation.dto.ReserveRequest;
import com.suchet.seat_reservation.security.BearerTokenFilter;
import com.suchet.seat_reservation.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/shows")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(
            ReservationService reservationService
    ) {
        this.reservationService = reservationService;
    }

    @PostMapping("/{showId}/reserve")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(
            @PathVariable UUID showId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReserveRequest request,
            HttpServletRequest httpRequest
    ) {
        String userId = (String) httpRequest.getAttribute(
                BearerTokenFilter.USER_ID_ATTRIBUTE
        );

        return reservationService.reserve(
                showId,
                userId,
                idempotencyKey,
                request
        );
    }
}
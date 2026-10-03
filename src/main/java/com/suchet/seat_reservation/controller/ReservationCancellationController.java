package com.suchet.seat_reservation.controller;

import com.suchet.seat_reservation.security.BearerTokenFilter;
import com.suchet.seat_reservation.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reservations")
public class ReservationCancellationController {

    private final ReservationService reservationService;

    public ReservationCancellationController(
            ReservationService reservationService
    ) {
        this.reservationService = reservationService;
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID reservationId,
            HttpServletRequest httpRequest
    ) {
        String userId = (String) httpRequest.getAttribute(
                BearerTokenFilter.USER_ID_ATTRIBUTE
        );

        reservationService.cancel(
                reservationId,
                userId
        );

        return ResponseEntity.noContent().build();
    }
}
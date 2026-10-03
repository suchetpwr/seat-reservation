package com.suchet.seat_reservation.controller;

import com.suchet.seat_reservation.dto.CreateShowRequest;
import com.suchet.seat_reservation.dto.ShowResponse;
import com.suchet.seat_reservation.service.ShowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.suchet.seat_reservation.dto.ShowDetailsResponse;
import java.util.UUID;

@RestController
@RequestMapping("/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponse createShow(
            @Valid @RequestBody CreateShowRequest request
    ) {
        return showService.createShow(request);
    }

    @GetMapping("/{showId}")
    public ShowDetailsResponse getShow(
            @PathVariable UUID showId
    ) {
        return showService.getShow(showId);
    }
}
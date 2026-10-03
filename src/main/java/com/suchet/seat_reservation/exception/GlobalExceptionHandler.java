package com.suchet.seat_reservation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReservationConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleReservationConflict(
            ReservationConflictException exception
    ) {
        return Map.of(
                "error", exception.getReason()
        );
    }

    @ExceptionHandler(ShowNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleShowNotFound(
            ShowNotFoundException exception
    ) {
        return Map.of(
                "error", "show_not_found"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return Map.of(
                "error", exception.getMessage()
        );
    }
}
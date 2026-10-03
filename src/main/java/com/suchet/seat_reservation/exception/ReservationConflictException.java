package com.suchet.seat_reservation.exception;

public class ReservationConflictException extends RuntimeException {

    private final String reason;

    public ReservationConflictException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
package com.suchet.seat_reservation.exception;

public class ShowNotFoundException extends RuntimeException {

    public ShowNotFoundException() {
        super("Show not found");
    }
}
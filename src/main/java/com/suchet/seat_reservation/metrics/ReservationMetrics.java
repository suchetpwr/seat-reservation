package com.suchet.seat_reservation.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ReservationMetrics {

    private final Counter confirmedReservations;
    private final Counter seatTakenDeclines;
    private final Counter perUserLimitDeclines;
    private final Counter idempotentReplays;

    public ReservationMetrics(MeterRegistry meterRegistry) {

        this.confirmedReservations =
                Counter.builder("seat_reservations_confirmed")
                        .description("Total confirmed seat reservations")
                        .register(meterRegistry);

        this.seatTakenDeclines =
                Counter.builder("seat_reservations_declined")
                        .description("Total declined seat reservations")
                        .tag("reason", "seat_taken")
                        .register(meterRegistry);

        this.perUserLimitDeclines =
                Counter.builder("seat_reservations_declined")
                        .description("Total declined seat reservations")
                        .tag("reason", "per_user_limit")
                        .register(meterRegistry);

        this.idempotentReplays =
                Counter.builder("seat_reservations_idempotent_replays")
                        .description("Total idempotent reservation replays")
                        .register(meterRegistry);
    }

    public void recordConfirmed() {
        confirmedReservations.increment();
    }

    public void recordSeatTakenDecline() {
        seatTakenDeclines.increment();
    }

    public void recordPerUserLimitDecline() {
        perUserLimitDeclines.increment();
    }

    public void recordIdempotentReplay() {
        idempotentReplays.increment();
    }
}
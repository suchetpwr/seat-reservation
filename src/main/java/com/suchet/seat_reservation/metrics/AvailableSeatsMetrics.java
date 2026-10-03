package com.suchet.seat_reservation.metrics;

import com.suchet.seat_reservation.model.SeatStatus;
import com.suchet.seat_reservation.repository.SeatRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AvailableSeatsMetrics {

    private final SeatRepository seatRepository;

    public AvailableSeatsMetrics(
            SeatRepository seatRepository,
            MeterRegistry meterRegistry
    ) {
        this.seatRepository = seatRepository;

        Gauge.builder(
                        "available_seats",
                        this,
                        AvailableSeatsMetrics::getAvailableSeats
                )
                .description("Number of currently available seats")
                .register(meterRegistry);
    }

    private double getAvailableSeats() {
        /*
         * This gauge is intended to expose the current number
         * of available seats across all shows.
         */
        return seatRepository
                .countByStatus(SeatStatus.AVAILABLE);
    }
}
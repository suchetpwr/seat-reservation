package com.suchet.seat_reservation.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
        name = "seats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_show_seat",
                        columnNames = {"show_id", "seat_number"}
                )
        }
)
public class Seat {

    @Id
    private UUID id;

    @Column(name = "show_id", nullable = false)
    private UUID showId;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    public Seat() {
    }

    public Seat(UUID id, UUID showId, String seatNumber) {
        this.id = id;
        this.showId = showId;
        this.seatNumber = seatNumber;
        this.status = SeatStatus.AVAILABLE;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getShowId() {
        return showId;
    }

    public void setShowId(UUID showId) {
        this.showId = showId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }
}
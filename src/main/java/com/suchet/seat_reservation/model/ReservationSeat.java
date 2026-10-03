package com.suchet.seat_reservation.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "reservation_seats")
@IdClass(ReservationSeat.ReservationSeatId.class)
public class ReservationSeat {

    @Id
    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Id
    @Column(name = "seat_id", nullable = false)
    private UUID seatId;

    public ReservationSeat() {
    }

    public ReservationSeat(UUID reservationId, UUID seatId) {
        this.reservationId = reservationId;
        this.seatId = seatId;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public void setReservationId(UUID reservationId) {
        this.reservationId = reservationId;
    }

    public UUID getSeatId() {
        return seatId;
    }

    public void setSeatId(UUID seatId) {
        this.seatId = seatId;
    }

    public static class ReservationSeatId implements Serializable {

        private UUID reservationId;
        private UUID seatId;

        public ReservationSeatId() {
        }

        public ReservationSeatId(UUID reservationId, UUID seatId) {
            this.reservationId = reservationId;
            this.seatId = seatId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ReservationSeatId that)) return false;

            return reservationId.equals(that.reservationId)
                    && seatId.equals(that.seatId);
        }

        @Override
        public int hashCode() {
            return 31 * reservationId.hashCode() + seatId.hashCode();
        }
    }
}
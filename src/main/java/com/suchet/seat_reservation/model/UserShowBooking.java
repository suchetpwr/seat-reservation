package com.suchet.seat_reservation.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "user_show_bookings")
@IdClass(UserShowBooking.UserShowBookingId.class)
public class UserShowBooking {

    @Id
    @Column(name = "show_id", nullable = false)
    private UUID showId;

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "booked_seats", nullable = false)
    private int bookedSeats = 0;

    public UserShowBooking() {
    }

    public UserShowBooking(UUID showId, String userId, int bookedSeats) {
        this.showId = showId;
        this.userId = userId;
        this.bookedSeats = bookedSeats;
    }

    public UUID getShowId() {
        return showId;
    }

    public void setShowId(UUID showId) {
        this.showId = showId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getBookedSeats() {
        return bookedSeats;
    }

    public void setBookedSeats(int bookedSeats) {
        this.bookedSeats = bookedSeats;
    }

    public static class UserShowBookingId implements Serializable {

        private UUID showId;
        private String userId;

        public UserShowBookingId() {
        }

        public UserShowBookingId(UUID showId, String userId) {
            this.showId = showId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UserShowBookingId that)) return false;

            return showId.equals(that.showId)
                    && userId.equals(that.userId);
        }

        @Override
        public int hashCode() {
            return 31 * showId.hashCode() + userId.hashCode();
        }
    }
}
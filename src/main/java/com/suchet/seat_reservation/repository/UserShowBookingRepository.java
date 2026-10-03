package com.suchet.seat_reservation.repository;

import com.suchet.seat_reservation.model.UserShowBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserShowBookingRepository
        extends JpaRepository<UserShowBooking, UserShowBooking.UserShowBookingId> {

    Optional<UserShowBooking> findByShowIdAndUserId(
            UUID showId,
            String userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT u
    FROM UserShowBooking u
    WHERE u.showId = :showId
      AND u.userId = :userId
""")
    Optional<UserShowBooking> findForUpdate(
            @Param("showId") UUID showId,
            @Param("userId") String userId
    );

    @Modifying
    @Query(value = """
    INSERT INTO user_show_bookings (show_id, user_id, booked_seats)
    VALUES (:showId, :userId, 0)
    ON CONFLICT (show_id, user_id)
    DO NOTHING
    """, nativeQuery = true)
    int createIfMissing(
            @Param("showId") UUID showId,
            @Param("userId") String userId
    );

    @Modifying
    @Query("""
    UPDATE UserShowBooking u
    SET u.bookedSeats = u.bookedSeats - :seatCount
    WHERE u.showId = :showId
      AND u.userId = :userId
      AND u.bookedSeats >= :seatCount
""")
    int decrementBookedSeats(
            @Param("showId") UUID showId,
            @Param("userId") String userId,
            @Param("seatCount") int seatCount
    );
}
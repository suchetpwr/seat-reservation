package com.suchet.seat_reservation.repository;

import com.suchet.seat_reservation.model.Seat;
import com.suchet.seat_reservation.model.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByShowId(UUID showId);

    List<Seat> findByShowIdAndSeatNumberIn(
            UUID showId,
            List<String> seatNumbers
    );

    long countByShowIdAndStatus(
            UUID showId,
            SeatStatus status
    );

    long countByStatus(SeatStatus status);
    @Modifying
    @Query("""
    UPDATE Seat s
    SET s.status = com.suchet.seat_reservation.model.SeatStatus.CONFIRMED
    WHERE s.id = :seatId
      AND s.status = com.suchet.seat_reservation.model.SeatStatus.AVAILABLE
""")
    int confirmIfAvailable(@Param("seatId") UUID seatId);

    @Modifying
    @Query("""
    UPDATE Seat s
    SET s.status = com.suchet.seat_reservation.model.SeatStatus.AVAILABLE
    WHERE s.id = :seatId
      AND s.status = com.suchet.seat_reservation.model.SeatStatus.CONFIRMED
""")
    int releaseIfConfirmed(@Param("seatId") UUID seatId);
}

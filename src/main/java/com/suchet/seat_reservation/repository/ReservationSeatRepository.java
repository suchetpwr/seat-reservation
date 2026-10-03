package com.suchet.seat_reservation.repository;

import com.suchet.seat_reservation.model.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.List;
import java.util.UUID;

public interface ReservationSeatRepository
        extends JpaRepository<ReservationSeat, ReservationSeat.ReservationSeatId> {

    List<ReservationSeat> findByReservationId(UUID reservationId);

    @Query("""
    SELECT rs.seatId
    FROM ReservationSeat rs
    WHERE rs.reservationId = :reservationId
""")
    List<UUID> findSeatIdsByReservationId(
            @Param("reservationId") UUID reservationId
    );
}
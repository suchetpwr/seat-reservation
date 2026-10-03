package com.suchet.seat_reservation.repository;

import com.suchet.seat_reservation.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByShowIdAndUserIdAndIdempotencyKey(
            UUID showId,
            String userId,
            String idempotencyKey
    );

    long countByShowIdAndUserIdAndStatus(
            UUID showId,
            String userId,
            String status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Reservation> findForUpdateById(UUID id);
}
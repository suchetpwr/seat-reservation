package com.suchet.seat_reservation.repository;

import com.suchet.seat_reservation.model.Show;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShowRepository extends JpaRepository<Show, UUID> {
}
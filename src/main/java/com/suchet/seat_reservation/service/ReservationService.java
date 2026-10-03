package com.suchet.seat_reservation.service;

import com.suchet.seat_reservation.dto.ReservationResponse;
import com.suchet.seat_reservation.dto.ReserveRequest;
import com.suchet.seat_reservation.exception.ReservationConflictException;
import com.suchet.seat_reservation.exception.ShowNotFoundException;
import com.suchet.seat_reservation.metrics.ReservationMetrics;
import com.suchet.seat_reservation.model.Reservation;
import com.suchet.seat_reservation.model.ReservationSeat;
import com.suchet.seat_reservation.model.ReservationStatus;
import com.suchet.seat_reservation.model.Show;
import com.suchet.seat_reservation.repository.ReservationRepository;
import com.suchet.seat_reservation.repository.ReservationSeatRepository;
import com.suchet.seat_reservation.repository.SeatRepository;
import com.suchet.seat_reservation.repository.ShowRepository;
import com.suchet.seat_reservation.repository.UserShowBookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReservationService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final UserShowBookingRepository userShowBookingRepository;
    private final RequestHashService requestHashService;
    private final ReservationMetrics reservationMetrics;

    private ReservationResponse buildReservationResponse(
            Reservation reservation
    ) {
        var seatIds = reservationSeatRepository
                .findSeatIdsByReservationId(reservation.getId());

        var seats = seatRepository.findAllById(seatIds)
                .stream()
                .map(seat -> seat.getSeatNumber())
                .sorted()
                .toList();

        return new ReservationResponse(
                reservation.getId(),
                reservation.getShowId(),
                reservation.getUserId(),
                seats,
                reservation.getAmountPaise(),
                reservation.getStatus().name()
        );
    }

    public ReservationService(
            ShowRepository showRepository,
            SeatRepository seatRepository,
            ReservationRepository reservationRepository,
            ReservationSeatRepository reservationSeatRepository,
            UserShowBookingRepository userShowBookingRepository,
            RequestHashService requestHashService,
            ReservationMetrics reservationMetrics
    ) {
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.userShowBookingRepository = userShowBookingRepository;
        this.requestHashService = requestHashService;
        this.reservationMetrics = reservationMetrics;
    }

    @Transactional
    public ReservationResponse reserve(
            UUID showId,
            String userId,
            String idempotencyKey,
            ReserveRequest request
    ) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ReservationConflictException(
                    "invalid_idempotency_key"
            );
        }

        Show show = showRepository.findById(showId)
                .orElseThrow(ShowNotFoundException::new);

        /*
         * Create the per-user booking row if it does not already exist.
         */
        userShowBookingRepository.createIfMissing(showId, userId);

        /*
         * Lock the user's booking row.
         *
         * This serializes concurrent reservations for the same
         * user and show. It also makes the idempotency check below
         * safe against concurrent identical requests.
         */
        var userBooking = userShowBookingRepository
                .findForUpdate(showId, userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "User booking row was not created"
                        )
                );

        /*
         * Idempotency check happens AFTER acquiring the user/show lock.
         */
        String requestHash = requestHashService.hash(request);

        var existingReservation =
                reservationRepository.findByShowIdAndUserIdAndIdempotencyKey(
                        showId,
                        userId,
                        idempotencyKey
                );

        if (existingReservation.isPresent()) {

            if (!existingReservation.get()
                    .getRequestHash()
                    .equals(requestHash)) {

                throw new ReservationConflictException(
                        "idempotency_conflict"
                );
            }

            reservationMetrics.recordIdempotentReplay();

            return buildReservationResponse(
                    existingReservation.get()
            );
        }

        /*
         * Normalize and validate requested seats.
         */
        var requestedSeats = request.seats()
                .stream()
                .distinct()
                .sorted()
                .toList();

        if (requestedSeats.size() != request.seats().size()) {

            throw new ReservationConflictException(
                    "duplicate_seat"
            );
        }

        int requestedSeatCount = requestedSeats.size();

        /*
         * Enforce the per-user booking limit while holding
         * the user/show row lock.
         */
        if (userBooking.getBookedSeats() + requestedSeatCount
                > show.getPerUserLimit()) {

            reservationMetrics.recordPerUserLimitDecline();

            throw new ReservationConflictException(
                    "per_user_limit"
            );
        }

        /*
         * Find all requested seats belonging to this show.
         */
        var seats = seatRepository.findByShowIdAndSeatNumberIn(
                showId,
                requestedSeats
        );

        if (seats.size() != requestedSeats.size()) {

            throw new ReservationConflictException(
                    "seat_not_found"
            );
        }

        /*
         * Always process seats in deterministic order.
         */
        seats.sort(
                java.util.Comparator.comparing(
                        com.suchet.seat_reservation.model.Seat::getSeatNumber
                )
        );

        /*
         * Atomically claim every requested seat.
         *
         * If even one seat is already taken, the exception causes
         * the entire transaction to roll back.
         */
        for (var seat : seats) {

            int updated = seatRepository.confirmIfAvailable(
                    seat.getId()
            );

            if (updated != 1) {

                reservationMetrics.recordSeatTakenDecline();

                throw new ReservationConflictException(
                        "seat_taken"
                );
            }
        }

        /*
         * Create one reservation for the complete request.
         */
        UUID reservationId = UUID.randomUUID();

        long amountPaise =
                show.getPricePaise() * requestedSeatCount;

        Reservation reservation = new Reservation(
                reservationId,
                showId,
                userId,
                amountPaise,
                ReservationStatus.CONFIRMED,
                idempotencyKey,
                requestHash
        );

        reservationRepository.save(reservation);

        /*
         * Link every claimed seat to this reservation.
         */
        var reservationSeats = seats.stream()
                .map(s ->
                        new ReservationSeat(
                                reservationId,
                                s.getId()
                        )
                )
                .toList();

        reservationSeatRepository.saveAll(reservationSeats);

        /*
         * Update the user's booked-seat counter.
         */
        userBooking.setBookedSeats(
                userBooking.getBookedSeats()
                        + requestedSeatCount
        );

        userShowBookingRepository.save(userBooking);

        reservationMetrics.recordConfirmed();

        return buildReservationResponse(reservation);
    }

    @Transactional
    public void cancel(
            UUID reservationId,
            String userId
    ) {

        /*
         * Lock the reservation so two cancellation requests
         * cannot process it simultaneously.
         */
        Reservation reservation = reservationRepository
                .findForUpdateById(reservationId)
                .orElseThrow(() ->
                        new ReservationConflictException(
                                "reservation_not_found"
                        )
                );

        /*
         * Only the reservation owner can cancel it.
         */
        if (!reservation.getUserId().equals(userId)) {

            throw new ReservationConflictException(
                    "not_reservation_owner"
            );
        }

        /*
         * A cancelled reservation cannot be cancelled again.
         */
        if (reservation.getStatus()
                == ReservationStatus.CANCELLED) {

            throw new ReservationConflictException(
                    "already_cancelled"
            );
        }

        /*
         * Find all seats belonging to this reservation.
         */
        var seatIds = reservationSeatRepository
                .findSeatIdsByReservationId(reservationId);

        /*
         * Release every seat atomically.
         *
         * CONFIRMED -> AVAILABLE
         */
        for (UUID seatId : seatIds) {

            int updated = seatRepository.releaseIfConfirmed(
                    seatId
            );

            if (updated != 1) {

                throw new IllegalStateException(
                        "Failed to release seat " + seatId
                );
            }
        }

        int releasedSeats = seatIds.size();

        /*
         * Decrement the user's booking counter.
         */
        int updated = userShowBookingRepository
                .decrementBookedSeats(
                        reservation.getShowId(),
                        userId,
                        releasedSeats
                );

        if (updated != 1) {

            throw new IllegalStateException(
                    "Failed to decrement user booking count"
            );
        }

        /*
         * Finally mark the reservation as cancelled.
         */
        reservation.setStatus(
                ReservationStatus.CANCELLED
        );

        reservationRepository.save(reservation);
    }
}
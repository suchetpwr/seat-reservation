package com.suchet.seat_reservation.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
        name = "reservations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_user_idempotency_key",
                        columnNames = {"show_id", "user_id", "idempotency_key"}
                )
        }
)
public class Reservation {

    @Id
    private UUID id;

    @Column(name = "show_id", nullable = false)
    private UUID showId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    public Reservation() {
    }

    public Reservation(
            UUID id,
            UUID showId,
            String userId,
            long amountPaise,
            ReservationStatus status,
            String idempotencyKey,
            String requestHash
    ) {
        this.id = id;
        this.showId = showId;
        this.userId = userId;
        this.amountPaise = amountPaise;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(long amountPaise) {
        this.amountPaise = amountPaise;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }
}
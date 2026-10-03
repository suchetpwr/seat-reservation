-- Store a fingerprint of the original reservation request.
-- This lets us detect: same idempotency key + different request body.
ALTER TABLE reservations
    ADD COLUMN request_hash VARCHAR(64) NOT NULL DEFAULT '';

-- The seat itself is the concurrency authority.
-- reservation_seats is historical linkage, so a seat can appear
-- in multiple reservations over time after cancellation.
ALTER TABLE reservation_seats
DROP CONSTRAINT unique_reserved_seat;
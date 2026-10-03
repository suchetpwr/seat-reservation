CREATE TABLE shows (
                       id UUID PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       price_paise BIGINT NOT NULL CHECK (price_paise >= 0),
                       per_user_limit INTEGER NOT NULL DEFAULT 4 CHECK (per_user_limit > 0),
                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seats (
                       id UUID PRIMARY KEY,
                       show_id UUID NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
                       seat_number VARCHAR(50) NOT NULL,
                       status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
                       CONSTRAINT seats_status_check
                           CHECK (status IN ('AVAILABLE', 'HELD', 'CONFIRMED')),
                       CONSTRAINT unique_show_seat
                           UNIQUE (show_id, seat_number)
);

CREATE TABLE reservations (
                              id UUID PRIMARY KEY,
                              show_id UUID NOT NULL REFERENCES shows(id),
                              user_id VARCHAR(255) NOT NULL,
                              amount_paise BIGINT NOT NULL CHECK (amount_paise >= 0),
                              status VARCHAR(20) NOT NULL,
                              idempotency_key VARCHAR(255) NOT NULL,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT reservations_status_check
                                  CHECK (status IN ('CONFIRMED', 'CANCELLED')),

                              CONSTRAINT unique_user_idempotency_key
                                  UNIQUE (show_id, user_id, idempotency_key)
);

CREATE TABLE reservation_seats (
                                   reservation_id UUID NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
                                   seat_id UUID NOT NULL REFERENCES seats(id),

                                   PRIMARY KEY (reservation_id, seat_id),

                                   CONSTRAINT unique_reserved_seat
                                       UNIQUE (seat_id)
);

CREATE INDEX idx_seats_show_status
    ON seats(show_id, status);

CREATE INDEX idx_reservations_show_user
    ON reservations(show_id, user_id);
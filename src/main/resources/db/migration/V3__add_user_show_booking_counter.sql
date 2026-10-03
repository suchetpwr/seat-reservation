CREATE TABLE user_show_bookings (
                                    show_id UUID NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
                                    user_id VARCHAR(255) NOT NULL,
                                    booked_seats INTEGER NOT NULL DEFAULT 0 CHECK (booked_seats >= 0),

                                    PRIMARY KEY (show_id, user_id)
);
-- Create booking table for seat reservations
CREATE TABLE booking (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    trip_id BIGINT NOT NULL REFERENCES trip(id),
    seat_label VARCHAR(10) NOT NULL,
    booked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(trip_id, seat_label)
);

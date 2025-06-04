-- Create Role Table
CREATE TABLE IF NOT EXISTS role (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) UNIQUE NOT NULL
);

-- Create User Table
CREATE TABLE IF NOT EXISTS ticket_user (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    role_id INT REFERENCES role(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Event Table
CREATE TABLE event (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    total_seats INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    booking_open BOOLEAN NOT NULL DEFAULT false,
    version BIGINT DEFAULT 0
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Create Ticket Table
CREATE TABLE ticket (
    booking_ref VARCHAR(64) PRIMARY KEY,
    event_id BIGINT REFERENCES event(id) ON DELETE CASCADE,
    user_id INT REFERENCES ticket_user(id) ON DELETE SET NULL,
    booking_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    price DECIMAL(10, 2) NOT NULL
);


-- Create Seat Table
CREATE TABLE IF NOT EXISTS seats (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT REFERENCES event(id) ON DELETE CASCADE,
    seat_id VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    booking_ref VARCHAR(64),
    UNIQUE(event_id, seat_id)
);

-- Create Indexes
CREATE INDEX IF NOT EXISTS idx_seat_event_id_seat_id ON seats (event_id, seat_id);
CREATE INDEX IF NOT EXISTS idx_seat_booking_ref ON seats (booking_ref);

CREATE INDEX IF NOT EXISTS idx_ticket_user_id ON ticket (user_id);
CREATE INDEX IF NOT EXISTS idx_ticket_event_id ON ticket (event_id);
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
CREATE TABLE IF NOT EXISTS event (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    location VARCHAR(200),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    created_by INT REFERENCES ticket_user(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Ticket Table
CREATE TABLE IF NOT EXISTS ticket (
    id SERIAL PRIMARY KEY,
    event_id INT REFERENCES event(id),
    price DECIMAL(10, 2) NOT NULL,
    available INT NOT NULL
);

-- Create Booking Table
CREATE TABLE IF NOT EXISTS booking (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES ticket_user(id),
    ticket_id INT REFERENCES ticket(id),
    quantity INT NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    booking_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Payment Table
CREATE TABLE IF NOT EXISTS payment (
    id SERIAL PRIMARY KEY,
    booking_id INT REFERENCES booking(id),
    amount DECIMAL(10, 2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    payment_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

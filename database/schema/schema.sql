-- Movie Ticket Booking Platform Database Schema
-- PostgreSQL

-- ============ AUTHENTICATION & USERS ============

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    user_type VARCHAR(50) NOT NULL CHECK (user_type IN ('CUSTOMER', 'PARTNER')),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    kyc_status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_status ON users(status);

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Platform administrator'),
    ('PARTNER', 'Theatre partner'),
    ('CUSTOMER', 'End customer'),
    ('SUPPORT', 'Support team member');

CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

CREATE TABLE tokens_blacklist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token TEXT UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    revoked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_tokens_blacklist_expires_at ON tokens_blacklist(expires_at);

-- ============ THEATRES & SCREENS ============

CREATE TABLE theatres (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'India',
    address TEXT NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    phone VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(255),
    kyc_status VARCHAR(50) DEFAULT 'PENDING',
    kyc_document_url VARCHAR(500),
    verification_date TIMESTAMP,
    total_screens INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_theatres_partner_id ON theatres(partner_id);
CREATE INDEX idx_theatres_city ON theatres(city);
CREATE INDEX idx_theatres_status ON theatres(status);
CREATE INDEX idx_theatres_location ON theatres(latitude, longitude);

CREATE TABLE screens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    theatre_id UUID NOT NULL REFERENCES theatres(id) ON DELETE CASCADE,
    screen_number INTEGER NOT NULL,
    total_seats INTEGER NOT NULL,
    seat_layout JSONB NOT NULL,
    premium_seat_count INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(theatre_id, screen_number)
);

CREATE INDEX idx_screens_theatre_id ON screens(theatre_id);

-- ============ MOVIES & SHOWS ============

CREATE TABLE movies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    genre VARCHAR(500) NOT NULL,
    languages VARCHAR(500) NOT NULL,
    duration_minutes INTEGER,
    release_date DATE,
    rating VARCHAR(10),
    imdb_rating DECIMAL(3, 1),
    poster_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_movies_title ON movies(title);

CREATE TABLE shows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    screen_id UUID NOT NULL REFERENCES screens(id) ON DELETE CASCADE,
    theatre_id UUID NOT NULL REFERENCES theatres(id) ON DELETE CASCADE,
    show_time TIMESTAMP NOT NULL,
    base_price DECIMAL(10, 2) NOT NULL,
    total_seats INTEGER NOT NULL,
    available_seats INTEGER NOT NULL,
    status VARCHAR(50) DEFAULT 'SCHEDULED',
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shows_theatre_date ON shows(theatre_id, show_time);
CREATE INDEX idx_shows_movie_id ON shows(movie_id);
CREATE INDEX idx_shows_status ON shows(status);

-- ============ SEAT INVENTORY ============

CREATE TABLE seat_inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    show_id UUID NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
    seat_number VARCHAR(10) NOT NULL,
    seat_type VARCHAR(50) DEFAULT 'STANDARD',
    status VARCHAR(50) DEFAULT 'AVAILABLE',
    locked_until TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(show_id, seat_number)
);

CREATE INDEX idx_seat_inventory_show_id ON seat_inventory(show_id);
CREATE INDEX idx_seat_inventory_show_status ON seat_inventory(show_id, status);

CREATE TABLE seat_locks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    show_id UUID NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
    seat_number VARCHAR(10) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    released_at TIMESTAMP,
    UNIQUE(show_id, seat_number)
);

CREATE INDEX idx_seat_locks_expires_at ON seat_locks(expires_at);

-- ============ BOOKINGS ============

CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    show_id UUID NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
    theatre_id UUID NOT NULL REFERENCES theatres(id),
    booking_reference VARCHAR(20) UNIQUE NOT NULL,
    booking_status VARCHAR(50) DEFAULT 'INITIATED',
    total_amount DECIMAL(10, 2) NOT NULL,
    final_amount DECIMAL(10, 2) NOT NULL,
    discount_applied DECIMAL(10, 2) DEFAULT 0,
    booking_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirmation_time TIMESTAMP,
    cancellation_time TIMESTAMP,
    cancellation_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bookings_user_status ON bookings(user_id, booking_status);
CREATE INDEX idx_bookings_show_id ON bookings(show_id);
CREATE INDEX idx_bookings_booking_reference ON bookings(booking_reference);

CREATE TABLE booking_seats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    screen_id UUID NOT NULL REFERENCES screens(id),
    seat_number VARCHAR(10) NOT NULL,
    seat_type VARCHAR(50) DEFAULT 'STANDARD',
    original_price DECIMAL(10, 2) NOT NULL,
    discount_percentage DECIMAL(5, 2) DEFAULT 0,
    final_price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'ALLOCATED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(booking_id, seat_number)
);

CREATE INDEX idx_booking_seats_booking_id ON booking_seats(booking_id);

-- ============ PAYMENTS & TRANSACTIONS ============

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'INR',
    payment_method VARCHAR(50) NOT NULL,
    gateway_name VARCHAR(50),
    gateway_transaction_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_booking_id ON transactions(booking_id);
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_status ON transactions(status);

CREATE TABLE refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    original_amount DECIMAL(10, 2) NOT NULL,
    refund_amount DECIMAL(10, 2) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'INITIATED',
    gateway_refund_id VARCHAR(255),
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_refunds_transaction_id ON refunds(transaction_id);
CREATE INDEX idx_refunds_status ON refunds(status);

CREATE TABLE settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    theatre_id UUID NOT NULL REFERENCES theatres(id),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_revenue DECIMAL(15, 2) NOT NULL,
    commission_percentage DECIMAL(5, 2) DEFAULT 10,
    platform_commission DECIMAL(15, 2) NOT NULL,
    theatre_amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'CALCULATED',
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(theatre_id, period_start, period_end)
);

CREATE INDEX idx_settlements_theatre_id ON settlements(theatre_id);

-- ============ OFFERS & PROMOTIONS ============

CREATE TABLE offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    offer_type VARCHAR(50) NOT NULL,
    value DECIMAL(10, 2) NOT NULL,
    max_discount_amount DECIMAL(10, 2),
    applicable_shows JSONB,
    applicable_user_segments JSONB,
    start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMP,
    max_usage_count INTEGER,
    usage_count INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_offers_active ON offers(active);
CREATE INDEX idx_offers_end_date ON offers(end_date);

CREATE TABLE promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    theatre_id UUID NOT NULL REFERENCES theatres(id),
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_percentage DECIMAL(5, 2),
    discount_amount DECIMAL(10, 2),
    max_discount_amount DECIMAL(10, 2),
    applicable_movie_ids VARCHAR(500),
    applicable_show_times VARCHAR(500),
    min_booking_amount DECIMAL(10, 2),
    max_usage INTEGER,
    current_usage INTEGER DEFAULT 0,
    valid_from TIMESTAMP NOT NULL,
    valid_till TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_promotions_code ON promotions(code);
CREATE INDEX idx_promotions_theatre_id ON promotions(theatre_id);

-- ============ NOTIFICATIONS ============

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    data JSONB,
    channel VARCHAR(50),
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    failed_reason TEXT
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);

CREATE TABLE notification_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    email_notifications BOOLEAN DEFAULT true,
    sms_notifications BOOLEAN DEFAULT true,
    push_notifications BOOLEAN DEFAULT true,
    offer_notifications BOOLEAN DEFAULT true,
    marketing_emails BOOLEAN DEFAULT false,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============ AUDIT TRAIL ============

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    changed_data JSONB,
    changed_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- Create Views for Analytics

CREATE VIEW booking_analytics AS
SELECT
    EXTRACT(DATE FROM b.booking_time) as booking_date,
    COUNT(*) as total_bookings,
    SUM(b.final_amount) as total_revenue,
    AVG(b.final_amount) as avg_booking_value,
    COUNT(DISTINCT b.user_id) as unique_customers
FROM bookings b
WHERE b.booking_status = 'CONFIRMED'
GROUP BY EXTRACT(DATE FROM b.booking_time);

CREATE VIEW theatre_performance AS
SELECT
    t.id,
    t.name,
    COUNT(DISTINCT b.id) as total_bookings,
    SUM(b.final_amount) as total_revenue,
    COUNT(DISTINCT b.user_id) as unique_customers,
    ROUND(
        (COUNT(CASE WHEN b.booking_status = 'CONFIRMED' THEN 1 END) * 100.0 / 
         NULLIF(COUNT(*), 0))::NUMERIC,
        2
    ) as booking_success_rate
FROM theatres t
LEFT JOIN bookings b ON t.id = b.theatre_id AND b.booking_time >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY t.id, t.name;

-- ============ CREATE SEQUENCES FOR BOOKING REFERENCE ============

CREATE SEQUENCE booking_reference_seq START 1000001;

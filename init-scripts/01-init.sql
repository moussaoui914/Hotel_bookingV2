-- ============================================================
-- INITIALISATION DE LA BASE DE DONNÉES HOTEL MANAGEMENT
-- Exécuté automatiquement par PostgreSQL au premier démarrage
-- ============================================================

-- ============================================================
-- 1. TYPES ENUM
-- ============================================================

CREATE TYPE user_role AS ENUM ('CLIENT', 'ADMIN');
CREATE TYPE room_type AS ENUM ('SINGLE', 'DOUBLE', 'SUITE');
CREATE TYPE room_status AS ENUM ('AVAILABLE', 'MAINTENANCE');
CREATE TYPE reservation_status AS ENUM ('CONFIRMED', 'CANCELLED', 'COMPLETED');
CREATE TYPE payment_method AS ENUM ('CASH', 'CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'ONLINE');
CREATE TYPE payment_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED');

-- ============================================================
-- 2. TABLES
-- ============================================================

-- ------------------------------------------------------------
-- TABLE: users
-- ------------------------------------------------------------
CREATE TABLE users (
                       id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       full_name       VARCHAR(150)    NOT NULL,
                       email           VARCHAR(255)    NOT NULL UNIQUE,
                       phone           VARCHAR(20)     NOT NULL,
                       password_hash   VARCHAR(255)    NOT NULL,
                       salt            VARCHAR(64)     NOT NULL,
                       role            user_role       NOT NULL DEFAULT 'CLIENT',
                       is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
                       created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_full_name_not_empty CHECK (LENGTH(TRIM(full_name)) > 0)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- ------------------------------------------------------------
-- TABLE: rooms
-- ------------------------------------------------------------
CREATE TABLE rooms (
                       room_number     VARCHAR(10)     PRIMARY KEY,
                       type            room_type       NOT NULL,
                       capacity        INTEGER         NOT NULL,
                       price_per_night NUMERIC(10, 2)  NOT NULL,
                       status          room_status     NOT NULL DEFAULT 'AVAILABLE',
                       floor           INTEGER,
                       description     TEXT,
                       created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT chk_capacity_positive CHECK (capacity > 0),
                       CONSTRAINT chk_price_positive CHECK (price_per_night > 0)
);

CREATE INDEX idx_rooms_type ON rooms(type);
CREATE INDEX idx_rooms_status ON rooms(status);
CREATE INDEX idx_rooms_price ON rooms(price_per_night);

-- ------------------------------------------------------------
-- TABLE: reservations
-- ------------------------------------------------------------
CREATE TABLE reservations (
                              id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              reservation_code    VARCHAR(20)         NOT NULL UNIQUE,
                              user_id             UUID                NOT NULL,
                              room_number         VARCHAR(10)         NOT NULL,
                              check_in            DATE                NOT NULL,
                              check_out           DATE                NOT NULL,
                              number_of_guests    INTEGER             NOT NULL,
                              number_of_nights    INTEGER             NOT NULL,
                              base_price          NUMERIC(10, 2)      NOT NULL,
                              total_price         NUMERIC(10, 2)      NOT NULL,
                              status              reservation_status  NOT NULL DEFAULT 'CONFIRMED',
                              created_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT fk_reservations_user FOREIGN KEY (user_id)
                                  REFERENCES users(id) ON DELETE RESTRICT,
                              CONSTRAINT fk_reservations_room FOREIGN KEY (room_number)
                                  REFERENCES rooms(room_number) ON DELETE RESTRICT,
                              CONSTRAINT chk_checkin_before_checkout CHECK (check_in < check_out),
                              CONSTRAINT chk_guests_positive CHECK (number_of_guests > 0),
                              CONSTRAINT chk_nights_positive CHECK (number_of_nights > 0),
                              CONSTRAINT chk_total_price_positive CHECK (total_price > 0),
                              CONSTRAINT chk_base_price_positive CHECK (base_price > 0)
);

CREATE INDEX idx_reservations_user ON reservations(user_id);
CREATE INDEX idx_reservations_room ON reservations(room_number);
CREATE INDEX idx_reservations_code ON reservations(reservation_code);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_dates ON reservations(check_in, check_out);
CREATE INDEX idx_reservations_room_dates_status
    ON reservations(room_number, check_in, check_out, status);

-- ------------------------------------------------------------
-- TABLE: payments
-- ------------------------------------------------------------
CREATE TABLE payments (
                          id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          reservation_id      UUID                NOT NULL,
                          amount              NUMERIC(10, 2)      NOT NULL,
                          payment_method      payment_method      NOT NULL,
                          status              payment_status      NOT NULL DEFAULT 'PENDING',
                          transaction_ref     VARCHAR(100),
                          payment_date        TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          refund_amount       NUMERIC(10, 2)      DEFAULT 0.00,
                          refund_date         TIMESTAMP,
                          notes               TEXT,
                          created_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT fk_payments_reservation FOREIGN KEY (reservation_id)
                              REFERENCES reservations(id) ON DELETE RESTRICT,
                          CONSTRAINT chk_amount_positive CHECK (amount > 0),
                          CONSTRAINT chk_refund_amount_positive CHECK (refund_amount >= 0),
                          CONSTRAINT chk_refund_not_exceed_amount CHECK (refund_amount <= amount)
);

CREATE INDEX idx_payments_reservation ON payments(reservation_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_date ON payments(payment_date);

-- ------------------------------------------------------------
-- TABLE: invoices
-- ------------------------------------------------------------
CREATE TABLE invoices (
                          id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          invoice_number      VARCHAR(30)         NOT NULL UNIQUE,
                          reservation_id      UUID                NOT NULL,
                          payment_id          UUID                NOT NULL,
                          subtotal_ht         NUMERIC(10, 2)      NOT NULL,
                          tva_rate            NUMERIC(5, 2)       NOT NULL DEFAULT 20.00,
                          tva_amount          NUMERIC(10, 2)      NOT NULL,
                          total_ttc           NUMERIC(10, 2)      NOT NULL,
                          issue_date          DATE                NOT NULL DEFAULT CURRENT_DATE,
                          due_date            DATE                NOT NULL,
                          is_paid             BOOLEAN             NOT NULL DEFAULT FALSE,
                          notes               TEXT,
                          created_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT fk_invoices_reservation FOREIGN KEY (reservation_id)
                              REFERENCES reservations(id) ON DELETE RESTRICT,
                          CONSTRAINT fk_invoices_payment FOREIGN KEY (payment_id)
                              REFERENCES payments(id) ON DELETE RESTRICT,
                          CONSTRAINT chk_subtotal_positive CHECK (subtotal_ht > 0),
                          CONSTRAINT chk_tva_amount_positive CHECK (tva_amount >= 0),
                          CONSTRAINT chk_total_positive CHECK (total_ttc > 0)
);

CREATE INDEX idx_invoices_number ON invoices(invoice_number);
CREATE INDEX idx_invoices_reservation ON invoices(reservation_id);
CREATE INDEX idx_invoices_date ON invoices(issue_date);

-- ============================================================
-- 3. FONCTIONS ET TRIGGERS : updated_at
-- ============================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_rooms_updated_at
    BEFORE UPDATE ON rooms
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_reservations_updated_at
    BEFORE UPDATE ON reservations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_invoices_updated_at
    BEFORE UPDATE ON invoices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 4. GÉNÉRATION AUTOMATIQUE DES CODES
-- ============================================================

-- Séquence pour les codes de réservation
CREATE SEQUENCE reservation_code_seq START 1;

CREATE OR REPLACE FUNCTION generate_reservation_code()
RETURNS VARCHAR AS $$
DECLARE
year_part VARCHAR(4);
    seq_part VARCHAR(4);
BEGIN
    year_part := TO_CHAR(CURRENT_DATE, 'YYYY');
    seq_part := LPAD(NEXTVAL('reservation_code_seq')::TEXT, 4, '0');
RETURN 'RES-' || year_part || '-' || seq_part;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION set_reservation_code()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.reservation_code IS NULL OR NEW.reservation_code = '' THEN
        NEW.reservation_code := generate_reservation_code();
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_set_reservation_code
    BEFORE INSERT ON reservations
    FOR EACH ROW EXECUTE FUNCTION set_reservation_code();

-- Séquence pour les numéros de facture
CREATE SEQUENCE invoice_number_seq START 1;

CREATE OR REPLACE FUNCTION generate_invoice_number()
RETURNS VARCHAR AS $$
DECLARE
year_part VARCHAR(4);
    seq_part VARCHAR(4);
BEGIN
    year_part := TO_CHAR(CURRENT_DATE, 'YYYY');
    seq_part := LPAD(NEXTVAL('invoice_number_seq')::TEXT, 4, '0');
RETURN 'INV-' || year_part || '-' || seq_part;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION set_invoice_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.invoice_number IS NULL OR NEW.invoice_number = '' THEN
        NEW.invoice_number := generate_invoice_number();
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_set_invoice_number
    BEFORE INSERT ON invoices
    FOR EACH ROW EXECUTE FUNCTION set_invoice_number();

-- ============================================================
-- 5. FONCTION DE VÉRIFICATION DE DISPONIBILITÉ
-- ============================================================

CREATE OR REPLACE FUNCTION check_room_availability(
    p_room_number VARCHAR,
    p_check_in DATE,
    p_check_out DATE,
    p_exclude_reservation_id UUID DEFAULT NULL
)
RETURNS BOOLEAN AS $$
DECLARE
overlap_count INTEGER;
BEGIN
SELECT COUNT(*) INTO overlap_count
FROM reservations
WHERE room_number = p_room_number
  AND status = 'CONFIRMED'
  AND (p_exclude_reservation_id IS NULL OR id != p_exclude_reservation_id)
  AND (
    p_check_in < check_out
        AND p_check_out > check_in
    );

RETURN overlap_count = 0;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- 6. VUES POUR LES RAPPORTS (KPI)
-- ============================================================

-- Vue : Taux d'occupation par chambre
CREATE OR REPLACE VIEW v_room_occupancy AS
SELECT
    r.room_number,
    r.type,
    COUNT(res.id) AS total_reservations,
    COALESCE(SUM(res.number_of_nights), 0) AS total_nights_booked,
    COALESCE(SUM(res.total_price), 0) AS total_revenue
FROM rooms r
         LEFT JOIN reservations res ON r.room_number = res.room_number
    AND res.status IN ('CONFIRMED', 'COMPLETED')
GROUP BY r.room_number, r.type
ORDER BY total_revenue DESC;

-- Vue : Chiffre d'affaires mensuel
CREATE OR REPLACE VIEW v_monthly_revenue AS
SELECT
    TO_CHAR(res.created_at, 'YYYY-MM') AS month,
    COUNT(res.id) AS total_reservations,
    COALESCE(SUM(res.total_price), 0) AS total_revenue,
    COALESCE(AVG(res.total_price), 0) AS average_basket
FROM reservations res
WHERE res.status IN ('CONFIRMED', 'COMPLETED')
GROUP BY TO_CHAR(res.created_at, 'YYYY-MM')
ORDER BY month DESC;

-- Vue : Top chambres
CREATE OR REPLACE VIEW v_top_rooms AS
SELECT
    r.room_number,
    r.type,
    COUNT(res.id) AS reservation_count,
    COALESCE(SUM(res.total_price), 0) AS total_revenue
FROM rooms r
         JOIN reservations res ON r.room_number = res.room_number
WHERE res.status IN ('CONFIRMED', 'COMPLETED')
GROUP BY r.room_number, r.type
ORDER BY reservation_count DESC
    LIMIT 10;

-- ============================================================
-- 7. DONNÉES DE TEST
-- ============================================================

-- Chambres de test
INSERT INTO rooms (room_number, type, capacity, price_per_night, status, floor, description) VALUES
                                                                                                 ('101', 'SINGLE', 1, 300.00, 'AVAILABLE', 1, 'Chambre simple confortable'),
                                                                                                 ('102', 'DOUBLE', 2, 500.00, 'AVAILABLE', 1, 'Chambre double avec vue jardin'),
                                                                                                 ('201', 'DOUBLE', 2, 550.00, 'AVAILABLE', 2, 'Chambre double supérieure'),
                                                                                                 ('205', 'SUITE', 4, 900.00, 'AVAILABLE', 2, 'Suite familiale spacieuse'),
                                                                                                 ('301', 'SUITE', 4, 1200.00, 'MAINTENANCE', 3, 'Suite présidentielle (en maintenance)');

-- ============================================================
-- FIN DU SCRIPT
-- ============================================================
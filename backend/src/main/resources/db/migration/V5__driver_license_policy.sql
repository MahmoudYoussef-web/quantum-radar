-- P5: driver / license / vehicle domain + tiered fine policy + optimistic-locking
-- columns. Penalty-point defaults for rules are set here (data change via a new
-- migration, never by editing V2).

CREATE TABLE drivers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    license_no VARCHAR(32) NOT NULL UNIQUE,
    penalty_points INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE licenses (
    id BIGSERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL UNIQUE REFERENCES drivers (id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL,
    issued_at DATE NOT NULL,
    expires_at DATE NOT NULL
);

CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    plate VARCHAR(32) NOT NULL UNIQUE,
    car_type VARCHAR(16) NOT NULL,
    driver_id BIGINT REFERENCES drivers (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_vehicles_driver ON vehicles (driver_id);

CREATE TABLE fine_tiers (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL,
    over_from INTEGER NOT NULL,
    over_to INTEGER,
    fee INTEGER NOT NULL,
    UNIQUE (rule_code, over_from)
);

ALTER TABLE fines ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE violations ADD COLUMN points INTEGER NOT NULL DEFAULT 0;

INSERT INTO fine_tiers (rule_code, over_from, over_to, fee) VALUES
    ('SPEED_LIMIT_PRIVATE', 1, 10, 300),
    ('SPEED_LIMIT_PRIVATE', 11, 30, 600),
    ('SPEED_LIMIT_PRIVATE', 31, NULL, 1000),
    ('SPEED_LIMIT_TRUCK', 1, 10, 300),
    ('SPEED_LIMIT_TRUCK', 11, 30, 600),
    ('SPEED_LIMIT_TRUCK', 31, NULL, 1000);

UPDATE rule_configs SET penalty_points = 1 WHERE code = 'SEATBELT';
UPDATE rule_configs SET penalty_points = 2 WHERE code IN ('SPEED_LIMIT_PRIVATE', 'SPEED_LIMIT_TRUCK', 'RESTRICTED_ZONE');
UPDATE rule_configs SET penalty_points = 3 WHERE code = 'RED_LIGHT';

INSERT INTO drivers (name, license_no, penalty_points) VALUES ('Ahmed Hassan', 'AHM-0001', 0);
INSERT INTO licenses (driver_id, status, issued_at, expires_at)
SELECT id, 'ACTIVE', DATE '2024-01-01', DATE '2034-01-01' FROM drivers WHERE license_no = 'AHM-0001';
INSERT INTO vehicles (plate, car_type, driver_id)
SELECT 'ABC1234', 'PRIVATE', id FROM drivers WHERE license_no = 'AHM-0001';
INSERT INTO vehicles (plate, car_type, driver_id)
SELECT 'XYZ777', 'TRUCK', id FROM drivers WHERE license_no = 'AHM-0001';

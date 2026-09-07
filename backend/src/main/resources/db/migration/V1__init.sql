-- P2 baseline schema. One Flyway file per phase: V1 here; P4 adds device (V2),
-- P5 adds driver/vehicle/license/fine-policy (V3), P6 adds refresh-token storage (V4).
-- Never hand-edit this file after it has been applied; add a new versioned migration.

CREATE TABLE observations (
    id BIGSERIAL PRIMARY KEY,
    plate_number VARCHAR(32) NOT NULL,
    observed_at DATE NOT NULL,
    car_type VARCHAR(16) NOT NULL,
    speed INTEGER NOT NULL,
    seatbelt_fastened BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_observations_plate ON observations (plate_number);

CREATE TABLE fines (
    id BIGSERIAL PRIMARY KEY,
    plate_number VARCHAR(32) NOT NULL,
    total_amount INTEGER NOT NULL,
    observation_id BIGINT REFERENCES observations (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_fines_plate ON fines (plate_number);

CREATE TABLE violations (
    id BIGSERIAL PRIMARY KEY,
    fine_id BIGINT NOT NULL REFERENCES fines (id) ON DELETE CASCADE,
    rule_name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NOT NULL,
    fee INTEGER NOT NULL
);
CREATE INDEX idx_violations_fine ON violations (fine_id);
CREATE INDEX idx_violations_rule ON violations (rule_name);

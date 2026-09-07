-- P4: device registry + idempotency key. The UNIQUE constraint on event_id IS the
-- duplicate guarantee (a resubmission fails closed with 409 instead of double-fining).
-- Backfill keeps the constraint strict (NOT NULL) even on dirty dev databases.

CREATE TABLE devices (
    id BIGSERIAL PRIMARY KEY,
    device_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO devices (device_code, name, active)
VALUES ('RADAR-001', 'Downtown radar 001', TRUE);

ALTER TABLE observations
    ADD COLUMN event_id VARCHAR(64),
    ADD COLUMN device_id BIGINT REFERENCES devices (id);

UPDATE observations SET event_id = 'backfill-' || id WHERE event_id IS NULL;

ALTER TABLE observations ALTER COLUMN event_id SET NOT NULL;
CREATE UNIQUE INDEX uq_observations_event_id ON observations (event_id);
CREATE INDEX idx_observations_device ON observations (device_id);

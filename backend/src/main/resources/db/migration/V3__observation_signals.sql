-- P3: signal fields needed by the new zone / red-light rules.
ALTER TABLE observations
    ADD COLUMN latitude DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION,
    ADD COLUMN light_state VARCHAR(8),
    ADD COLUMN crossed_stop_line BOOLEAN NOT NULL DEFAULT FALSE;

-- B3: device health. last_seen_at is bumped by accepted events AND explicit
-- heartbeats; the API derives ACTIVE/DEGRADED/OFFLINE from its age so health
-- is always computed, never stored (stored health would go stale).

ALTER TABLE devices
    ADD COLUMN last_seen_at TIMESTAMPTZ,
    ADD COLUMN firmware_version VARCHAR(32),
    ADD COLUMN last_ip VARCHAR(64);

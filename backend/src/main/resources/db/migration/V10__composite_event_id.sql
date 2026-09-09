-- B4: idempotency scope is (device, event), not event alone: two radars may
-- legitimately emit the same local sequence number. NULL device rows (pre-P4
-- history) stay distinct under Postgres NULL semantics, so no backfill needed.
-- Plus CHECK defenses the app layer already assumes: license status values and
-- non-negative money.

DROP INDEX IF EXISTS uq_observations_event_id;
CREATE UNIQUE INDEX uq_observations_device_event ON observations (device_id, event_id);

ALTER TABLE licenses
    ADD CONSTRAINT chk_license_status
    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED', 'EXPIRED'));

ALTER TABLE fines
    ADD CONSTRAINT chk_fine_total CHECK (total_amount >= 0);

ALTER TABLE violations
    ADD CONSTRAINT chk_violation_fee CHECK (fee >= 0);

-- B1: audit log. Every administrative mutation records who did what, the
-- before/after payload, and the caller IP. Application-layer writes (explicit
-- service calls, not DB triggers) so the audit trail is portable and testable.

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64),
    entity_id VARCHAR(128),
    details TEXT,
    ip_address VARCHAR(64)
);
CREATE INDEX idx_audit_occurred ON audit_log (occurred_at DESC);
CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);

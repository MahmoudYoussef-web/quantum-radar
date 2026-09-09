-- B2: rule versioning. Each tuning of a rule snapshots a new version row;
-- the engine always evaluates the latest version effective as of now, and each
-- violation pins the version number it was judged under — old fines never move
-- when a rule changes. v1 rows are seeded from the live configs.
-- NOTE: tier rows stay global per rule (live pricing policy). Violation rows
-- already pin their computed fee+points at write time, so history is immutable
-- without versioning the tiers themselves.

CREATE TABLE rule_versions (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL,
    version INTEGER NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL,
    fee INTEGER NOT NULL,
    penalty_points INTEGER NOT NULL DEFAULT 0,
    max_speed INTEGER,
    zone_min_lat DOUBLE PRECISION,
    zone_max_lat DOUBLE PRECISION,
    zone_min_lon DOUBLE PRECISION,
    zone_max_lon DOUBLE PRECISION,
    effective_from TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (rule_code, version)
);
CREATE INDEX idx_rule_versions_lookup ON rule_versions (rule_code, effective_from DESC);

INSERT INTO rule_versions
    (rule_code, version, display_name, enabled, fee, penalty_points, max_speed,
     zone_min_lat, zone_max_lat, zone_min_lon, zone_max_lon, effective_from)
SELECT code, 1, display_name, enabled, fee, penalty_points, max_speed,
       zone_min_lat, zone_max_lat, zone_min_lon, zone_max_lon, now()
FROM rule_configs;

ALTER TABLE violations ADD COLUMN rule_version INTEGER;

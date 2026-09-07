-- P3: DB-configurable rules. NOTE on numbering: the P2 commit message sketched
-- V2=device, V3=driver, V4=tokens, but rule_configs (P3) must come first, so the
-- chain shifts by one: V2/V3 here (P3), V4 device+event (P4), V5 driver/license (P5),
-- V6 users/tokens (P6). Versions only ever move forward; applied files are frozen.

CREATE TABLE rule_configs (
    code VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    fee INTEGER NOT NULL,
    penalty_points INTEGER NOT NULL DEFAULT 0,
    max_speed INTEGER,
    zone_min_lat DOUBLE PRECISION,
    zone_max_lat DOUBLE PRECISION,
    zone_min_lon DOUBLE PRECISION,
    zone_max_lon DOUBLE PRECISION
);

INSERT INTO rule_configs
    (code, display_name, enabled, fee, penalty_points, max_speed,
     zone_min_lat, zone_max_lat, zone_min_lon, zone_max_lon)
VALUES
    ('SEATBELT', 'Seatbelt', TRUE, 100, 0, NULL, NULL, NULL, NULL, NULL),
    ('SPEED_LIMIT_PRIVATE', 'Private speed limit', TRUE, 300, 0, 80, NULL, NULL, NULL, NULL),
    ('SPEED_LIMIT_TRUCK', 'Truck speed limit', TRUE, 300, 0, 60, NULL, NULL, NULL, NULL),
    ('RESTRICTED_ZONE', 'Restricted zone', TRUE, 500, 0, NULL, 30.00, 30.10, 31.10, 31.30),
    ('RED_LIGHT', 'Red light', TRUE, 500, 0, NULL, NULL, NULL, NULL, NULL);

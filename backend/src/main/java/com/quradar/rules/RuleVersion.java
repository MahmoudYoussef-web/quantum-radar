package com.quradar.rules;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "rule_versions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rule_code", "version"}))
public class RuleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(nullable = false)
    private int version;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int fee;

    @Column(name = "penalty_points", nullable = false)
    private int penaltyPoints;

    @Column(name = "max_speed")
    private Integer maxSpeed;

    @Column(name = "zone_min_lat")
    private Double zoneMinLat;

    @Column(name = "zone_max_lat")
    private Double zoneMaxLat;

    @Column(name = "zone_min_lon")
    private Double zoneMinLon;

    @Column(name = "zone_max_lon")
    private Double zoneMaxLon;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RuleVersion() {
    }

    public RuleVersion(String ruleCode, int version, RuleConfig config, Instant effectiveFrom) {
        this.ruleCode = ruleCode;
        this.version = version;
        this.displayName = config.getDisplayName();
        this.enabled = config.isEnabled();
        this.fee = config.getFee();
        this.penaltyPoints = config.getPenaltyPoints();
        this.maxSpeed = config.getMaxSpeed();
        this.zoneMinLat = config.getZoneMinLat();
        this.zoneMaxLat = config.getZoneMaxLat();
        this.zoneMinLon = config.getZoneMinLon();
        this.zoneMaxLon = config.getZoneMaxLon();
        this.effectiveFrom = effectiveFrom;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public int getVersion() {
        return version;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getFee() {
        return fee;
    }

    public int getPenaltyPoints() {
        return penaltyPoints;
    }

    public Integer getMaxSpeed() {
        return maxSpeed;
    }

    public Double getZoneMinLat() {
        return zoneMinLat;
    }

    public Double getZoneMaxLat() {
        return zoneMaxLat;
    }

    public Double getZoneMinLon() {
        return zoneMinLon;
    }

    public Double getZoneMaxLon() {
        return zoneMaxLon;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }
}

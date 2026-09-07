package com.quradar.rules;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rule_configs")
public class RuleConfig {

    @Id
    @Column(length = 64)
    private String code;

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

    protected RuleConfig() {
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getFee() {
        return fee;
    }

    public void setFee(int fee) {
        this.fee = fee;
    }

    public int getPenaltyPoints() {
        return penaltyPoints;
    }

    public void setPenaltyPoints(int penaltyPoints) {
        this.penaltyPoints = penaltyPoints;
    }

    public Integer getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(Integer maxSpeed) {
        this.maxSpeed = maxSpeed;
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
}

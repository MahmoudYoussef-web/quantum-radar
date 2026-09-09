package com.quradar.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "devices")
public class DeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_code", nullable = false, unique = true, length = 64)
    private String deviceCode;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "firmware_version", length = 32)
    private String firmwareVersion;

    @Column(name = "last_ip", length = 64)
    private String lastIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DeviceEntity() {
    }

    public DeviceEntity(String deviceCode, String name, boolean active) {
        this.deviceCode = deviceCode;
        this.name = name;
        this.active = active;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void seen(String firmwareVersion, String ip) {
        this.lastSeenAt = Instant.now();
        if (firmwareVersion != null) {
            this.firmwareVersion = firmwareVersion;
        }
        if (ip != null) {
            this.lastIp = ip;
        }
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public String getLastIp() {
        return lastIp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

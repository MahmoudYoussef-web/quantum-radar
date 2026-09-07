package com.quradar.driver;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "license_no", nullable = false, unique = true, length = 32)
    private String licenseNo;

    @Column(name = "penalty_points", nullable = false)
    private int penaltyPoints;

    @Version
    private Long version;

    protected Driver() {
    }

    public Driver(String name, String licenseNo) {
        this.name = name;
        this.licenseNo = licenseNo;
        this.penaltyPoints = 0;
    }

    public void addPenaltyPoints(int points) {
        this.penaltyPoints += points;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLicenseNo() {
        return licenseNo;
    }

    public int getPenaltyPoints() {
        return penaltyPoints;
    }

    public Long getVersion() {
        return version;
    }
}

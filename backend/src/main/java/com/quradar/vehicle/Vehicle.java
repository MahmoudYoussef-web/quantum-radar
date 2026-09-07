package com.quradar.vehicle;

import com.quradar.common.CarType;
import com.quradar.driver.Driver;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String plate;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_type", nullable = false, length = 16)
    private CarType carType;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Vehicle() {
    }

    public Vehicle(String plate, CarType carType, Driver owner) {
        this.plate = plate;
        this.carType = carType;
        this.owner = owner;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getPlate() {
        return plate;
    }

    public CarType getCarType() {
        return carType;
    }

    public Driver getOwner() {
        return owner;
    }
}

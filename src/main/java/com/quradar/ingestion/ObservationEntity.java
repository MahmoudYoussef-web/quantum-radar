package com.quradar.ingestion;

import com.quradar.common.CarType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "observations")
public class ObservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_number", nullable = false, length = 32)
    private String plateNumber;

    @Column(name = "observed_at", nullable = false)
    private LocalDate observedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_type", nullable = false, length = 16)
    private CarType carType;

    @Column(nullable = false)
    private int speed;

    @Column(name = "seatbelt_fastened", nullable = false)
    private boolean seatbeltFastened;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "light_state", length = 8)
    private LightState lightState;

    @Column(name = "crossed_stop_line", nullable = false)
    private boolean crossedStopLine;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ObservationEntity() {
    }

    public ObservationEntity(String plateNumber, LocalDate observedAt, CarType carType,
                             int speed, boolean seatbeltFastened, Double latitude,
                             Double longitude, LightState lightState, boolean crossedStopLine) {
        this.plateNumber = plateNumber;
        this.observedAt = observedAt;
        this.carType = carType;
        this.speed = speed;
        this.seatbeltFastened = seatbeltFastened;
        this.latitude = latitude;
        this.longitude = longitude;
        this.lightState = lightState;
        this.crossedStopLine = crossedStopLine;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public LocalDate getObservedAt() {
        return observedAt;
    }

    public CarType getCarType() {
        return carType;
    }

    public int getSpeed() {
        return speed;
    }

    public boolean isSeatbeltFastened() {
        return seatbeltFastened;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public LightState getLightState() {
        return lightState;
    }

    public boolean isCrossedStopLine() {
        return crossedStopLine;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

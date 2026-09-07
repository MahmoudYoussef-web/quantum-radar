package com.quradar.ingestion;

import com.quradar.common.CarType;
import java.time.LocalDate;

public class Observation {

    private String eventId;
    private String deviceCode;
    private String plateNumber;
    private LocalDate date;
    private CarType carType;
    private int speed;
    private boolean seatbeltFastened;
    private Double latitude;
    private Double longitude;
    private LightState lightState;
    private boolean crossedStopLine;

    public Observation(String eventId, String deviceCode, String plateNumber, LocalDate date,
                       CarType carType, int speed, boolean seatbeltFastened, Double latitude,
                       Double longitude, LightState lightState, boolean crossedStopLine) {
        this.eventId = eventId;
        this.deviceCode = deviceCode;
        this.plateNumber = plateNumber;
        this.date = date;
        this.carType = carType;
        this.speed = speed;
        this.seatbeltFastened = seatbeltFastened;
        this.latitude = latitude;
        this.longitude = longitude;
        this.lightState = lightState;
        this.crossedStopLine = crossedStopLine;
    }

    public String getEventId() {
        return eventId;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public LocalDate getDate() {
        return date;
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
}

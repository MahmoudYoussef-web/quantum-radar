package com.quradar.vehicle;

public class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(String plate) {
        super("Unknown vehicle: " + plate);
    }
}

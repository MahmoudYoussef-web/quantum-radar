package com.quradar.driver;

public class DriverNotFoundException extends RuntimeException {

    public DriverNotFoundException(String licenseNo) {
        super("Unknown driver: " + licenseNo);
    }
}

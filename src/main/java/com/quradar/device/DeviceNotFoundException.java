package com.quradar.device;

public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(String deviceCode) {
        super("Unknown device: " + deviceCode);
    }
}

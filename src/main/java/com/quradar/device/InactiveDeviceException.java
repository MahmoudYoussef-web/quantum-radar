package com.quradar.device;

public class InactiveDeviceException extends RuntimeException {

    public InactiveDeviceException(String deviceCode) {
        super("Device deactivated: " + deviceCode);
    }
}

package com.quradar.security;

import com.quradar.vehicle.Vehicle;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Server-side ownership checks. Controllers call these with DB-loaded data;
 * client-supplied identity is never trusted.
 */
@Component
public class SecuritySupport {

    public AppPrincipal current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Not authenticated");
    }

    /** DEVICE submits only for its own device; ADMIN may submit for any. */
    public void requireDeviceOwner(String deviceCode) {
        AppPrincipal principal = current();
        if (principal.role() == Role.ADMIN) {
            return;
        }
        if (principal.role() == Role.DEVICE && deviceCode.equals(principal.deviceCode())) {
            return;
        }
        throw new AccessDeniedException("Devices may only submit for themselves");
    }

    /** CITIZEN sees only their own driver record; ADMIN/OFFICER see any. */
    public void requireDriverOwner(String licenseNo) {
        AppPrincipal principal = current();
        if (principal.role() == Role.ADMIN || principal.role() == Role.OFFICER) {
            return;
        }
        if (principal.role() == Role.CITIZEN && licenseNo.equals(principal.driverLicenseNo())) {
            return;
        }
        throw new AccessDeniedException("Citizens may only view their own records");
    }

    /** CITIZEN sees only plates owned by their driver record. */
    public void requireVehicleOwner(Vehicle vehicle) {
        AppPrincipal principal = current();
        if (principal.role() == Role.ADMIN || principal.role() == Role.OFFICER) {
            return;
        }
        if (principal.role() == Role.CITIZEN && vehicle.getOwner() != null
                && vehicle.getOwner().getLicenseNo().equals(principal.driverLicenseNo())) {
            return;
        }
        throw new AccessDeniedException("Citizens may only view their own vehicles");
    }
}

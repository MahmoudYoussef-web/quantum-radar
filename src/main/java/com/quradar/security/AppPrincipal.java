package com.quradar.security;

/**
 * Authenticated identity. Ownership checks compare THESE server-side values —
 * never anything the client sends in the request body.
 */
public record AppPrincipal(String username, Role role, String driverLicenseNo, String deviceCode) {
}

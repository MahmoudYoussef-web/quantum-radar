package com.quradar.rules;

import java.time.Instant;

public record RuleVersionResponse(String code, int version, boolean enabled, int fee,
        int penaltyPoints, Integer maxSpeed, Instant effectiveFrom) {

    public static RuleVersionResponse from(RuleVersion version) {
        return new RuleVersionResponse(version.getRuleCode(), version.getVersion(),
                version.isEnabled(), version.getFee(), version.getPenaltyPoints(),
                version.getMaxSpeed(), version.getEffectiveFrom());
    }
}

package com.quradar.rules;

public record RuleConfigResponse(
        String code,
        String displayName,
        boolean enabled,
        int fee,
        int penaltyPoints,
        Integer maxSpeed) {

    public static RuleConfigResponse from(RuleConfig config) {
        return new RuleConfigResponse(config.getCode(), config.getDisplayName(), config.isEnabled(),
                config.getFee(), config.getPenaltyPoints(), config.getMaxSpeed());
    }
}

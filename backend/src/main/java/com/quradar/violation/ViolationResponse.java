package com.quradar.violation;

public record ViolationResponse(String ruleName, String description, int fee, int points,
        int ruleVersion) {

    public static ViolationResponse from(com.quradar.violation.Violation violation) {
        return new ViolationResponse(violation.getRuleName(), violation.getDescription(),
                violation.getFee(), violation.getPoints(), violation.getRuleVersion());
    }
}

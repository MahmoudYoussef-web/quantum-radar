package com.quradar.violation;

public record ViolationResponse(String ruleName, String description, int fee) {

    public static ViolationResponse from(com.quradar.violation.Violation violation) {
        return new ViolationResponse(violation.getRuleName(), violation.getDescription(),
                violation.getFee());
    }
}

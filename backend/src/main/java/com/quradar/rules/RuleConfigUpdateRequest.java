package com.quradar.rules;

import jakarta.validation.constraints.Min;

public record RuleConfigUpdateRequest(
        Boolean enabled,
        @Min(0) Integer fee,
        @Min(0) Integer penaltyPoints,
        @Min(1) Integer maxSpeed) {
}

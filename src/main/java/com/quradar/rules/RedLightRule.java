package com.quradar.rules;

import com.quradar.ingestion.LightState;
import com.quradar.ingestion.Observation;
import com.quradar.violation.Violation;

public class RedLightRule implements ViolationRule {

    private final String code;
    private final int fee;

    public RedLightRule(String code, int fee) {
        this.code = code;
        this.fee = fee;
    }

    @Override
    public String getRuleCode() {
        return code;
    }

    @Override
    public boolean matches(Observation observation) {
        return observation.getLightState() == LightState.RED && observation.isCrossedStopLine();
    }

    @Override
    public Violation evaluate(Observation observation) {
        if (!matches(observation)) {
            return null;
        }
        return new Violation(code, "crossed stop line on red light", fee);
    }
}

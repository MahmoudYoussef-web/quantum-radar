package com.quradar.rules;

import com.quradar.ingestion.Observation;
import com.quradar.violation.Violation;

public class SeatbeltRule implements ViolationRule {

    private final String code;
    private final int fee;

    public SeatbeltRule(String code, int fee) {
        this.code = code;
        this.fee = fee;
    }

    @Override
    public String getRuleCode() {
        return code;
    }

    @Override
    public boolean matches(Observation observation) {
        return !observation.isSeatbeltFastened();
    }

    @Override
    public Violation evaluate(Observation observation) {
        if (!matches(observation)) {
            return null;
        }
        return new Violation(code, "Seatbelt not fastened", fee);
    }
}

package com.quradar.rules;

import com.quradar.common.CarType;
import com.quradar.ingestion.Observation;
import com.quradar.violation.Violation;

public class SpeedLimitRule implements ViolationRule {

    private final String code;
    private final CarType carType;
    private final int maxSpeed;
    private final int fee;

    public SpeedLimitRule(String code, CarType carType, int maxSpeed, int fee) {
        this.code = code;
        this.carType = carType;
        this.maxSpeed = maxSpeed;
        this.fee = fee;
    }

    public CarType getCarType() {
        return carType;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    @Override
    public String getRuleCode() {
        return code;
    }

    @Override
    public boolean matches(Observation observation) {
        return observation.getCarType() == carType && observation.getSpeed() > maxSpeed;
    }

    @Override
    public Violation evaluate(Observation observation) {
        if (!matches(observation)) {
            return null;
        }
        String description = "speed of " + observation.getSpeed() + " exceeded max allowed " + maxSpeed;
        return new Violation(code, description, fee);
    }
}

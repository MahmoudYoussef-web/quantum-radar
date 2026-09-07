package com.quradar.rules;

import com.quradar.ingestion.Observation;
import com.quradar.violation.Violation;

public class RestrictedZoneRule implements ViolationRule {

    private final String code;
    private final double minLat;
    private final double maxLat;
    private final double minLon;
    private final double maxLon;
    private final int fee;

    public RestrictedZoneRule(String code, double minLat, double maxLat,
                              double minLon, double maxLon, int fee) {
        this.code = code;
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.fee = fee;
    }

    @Override
    public String getRuleCode() {
        return code;
    }

    @Override
    public boolean matches(Observation observation) {
        Double lat = observation.getLatitude();
        Double lon = observation.getLongitude();
        if (lat == null || lon == null) {
            return false;
        }
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }

    @Override
    public Violation evaluate(Observation observation) {
        if (!matches(observation)) {
            return null;
        }
        return new Violation(code, "vehicle inside restricted zone", fee);
    }
}

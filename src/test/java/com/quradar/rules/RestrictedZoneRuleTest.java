package com.quradar.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quradar.common.CarType;
import com.quradar.ingestion.LightState;
import com.quradar.ingestion.Observation;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RestrictedZoneRuleTest {

    private final RestrictedZoneRule rule =
            new RestrictedZoneRule("RESTRICTED_ZONE", 30.00, 30.10, 31.10, 31.30, 500);

    private Observation observation(Double lat, Double lon) {
        return new Observation("P1", LocalDate.now(), CarType.PRIVATE, 50, true,
                lat, lon, LightState.GREEN, false);
    }

    @Test
    void matchesInsideBoundingBox() {
        assertTrue(rule.matches(observation(30.05, 31.20)));
    }

    @Test
    void matchesOnBoundary() {
        assertTrue(rule.matches(observation(30.00, 31.10)));
    }

    @Test
    void noMatchOutsideBox() {
        assertFalse(rule.matches(observation(29.90, 31.20)));
    }

    @Test
    void noMatchWithoutCoordinates() {
        assertFalse(rule.matches(observation(null, null)));
    }

    @Test
    void evaluateReturnsNullOutsideBox() {
        assertNull(rule.evaluate(observation(31.00, 32.00)));
    }
}

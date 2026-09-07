package com.quradar.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quradar.common.CarType;
import com.quradar.ingestion.LightState;
import com.quradar.ingestion.Observation;
import com.quradar.violation.Violation;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SpeedLimitRuleTest {

    private final SpeedLimitRule rule = new SpeedLimitRule("SPEED_LIMIT_PRIVATE", CarType.PRIVATE, 80, 300);

    private Observation observation(CarType type, int speed) {
        return new Observation("P1", LocalDate.now(), type, speed, true,
                null, null, LightState.GREEN, false);
    }

    @Test
    void matchesWhenOverLimitForConfiguredType() {
        assertTrue(rule.matches(observation(CarType.PRIVATE, 81)));
    }

    @Test
    void noMatchAtExactlyLimit() {
        assertFalse(rule.matches(observation(CarType.PRIVATE, 80)));
    }

    @Test
    void noMatchForOtherCarType() {
        assertFalse(rule.matches(observation(CarType.TRUCK, 120)));
    }

    @Test
    void evaluateBuildsViolationWithCodeAndFee() {
        Violation violation = rule.evaluate(observation(CarType.PRIVATE, 94));
        assertEquals("SPEED_LIMIT_PRIVATE", violation.getRuleName());
        assertEquals(300, violation.getFee());
    }

    @Test
    void evaluateReturnsNullWhenNoMatch() {
        assertNull(rule.evaluate(observation(CarType.PRIVATE, 60)));
    }
}

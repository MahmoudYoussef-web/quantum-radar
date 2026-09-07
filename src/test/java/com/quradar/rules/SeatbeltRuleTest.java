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

class SeatbeltRuleTest {

    private final SeatbeltRule rule = new SeatbeltRule("SEATBELT", 100);

    private Observation observation(boolean fastened) {
        return new Observation("P1", LocalDate.now(), CarType.PRIVATE, 50, fastened,
                null, null, LightState.GREEN, false);
    }

    @Test
    void matchesWhenBeltNotFastened() {
        assertTrue(rule.matches(observation(false)));
    }

    @Test
    void noMatchWhenBeltFastened() {
        assertFalse(rule.matches(observation(true)));
    }

    @Test
    void evaluateBuildsViolationWithCodeAndFee() {
        Violation violation = rule.evaluate(observation(false));
        assertEquals("SEATBELT", violation.getRuleName());
        assertEquals(100, violation.getFee());
    }

    @Test
    void evaluateReturnsNullWhenFastened() {
        assertNull(rule.evaluate(observation(true)));
    }
}

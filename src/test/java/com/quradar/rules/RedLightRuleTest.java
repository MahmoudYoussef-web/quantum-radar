package com.quradar.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quradar.common.CarType;
import com.quradar.ingestion.LightState;
import com.quradar.ingestion.Observation;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RedLightRuleTest {

    private final RedLightRule rule = new RedLightRule("RED_LIGHT", 500);

    private Observation observation(LightState light, boolean crossed) {
        return new Observation("evt-1", null, "P1", LocalDate.now(), CarType.PRIVATE, 40, true,
                null, null, light, crossed);
    }

    @Test
    void matchesOnRedWithCrossing() {
        assertTrue(rule.matches(observation(LightState.RED, true)));
    }

    @Test
    void noMatchOnRedWithoutCrossing() {
        assertFalse(rule.matches(observation(LightState.RED, false)));
    }

    @Test
    void noMatchOnGreenWithCrossing() {
        assertFalse(rule.matches(observation(LightState.GREEN, true)));
    }

    @Test
    void noMatchWithoutSignal() {
        assertFalse(rule.matches(observation(null, true)));
    }

    @Test
    void evaluateReturnsNullOnGreen() {
        assertNull(rule.evaluate(observation(LightState.GREEN, true)));
    }
}

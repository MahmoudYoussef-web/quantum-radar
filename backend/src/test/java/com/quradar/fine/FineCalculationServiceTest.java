package com.quradar.fine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.quradar.common.CarType;
import com.quradar.ingestion.LightState;
import com.quradar.ingestion.Observation;
import com.quradar.rules.SpeedLimitRule;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FineCalculationServiceTest {

    @Mock
    private FineTierRepository tiers;

    @InjectMocks
    private FineCalculationService service;

    private final SpeedLimitRule rule =
            new SpeedLimitRule("SPEED_LIMIT_PRIVATE", CarType.PRIVATE, 80, 300);

    private Observation observation(int speed) {
        return new Observation("evt-1", null, "P1", LocalDate.now(), CarType.PRIVATE, speed,
                true, null, null, LightState.GREEN, false);
    }

    @Test
    void picksTierByOverLimit() {
        when(tiers.findByRuleCodeOrderByOverFromAsc("SPEED_LIMIT_PRIVATE")).thenReturn(List.of(
                new FineTier("SPEED_LIMIT_PRIVATE", 1, 10, 300),
                new FineTier("SPEED_LIMIT_PRIVATE", 11, 30, 600),
                new FineTier("SPEED_LIMIT_PRIVATE", 31, null, 1000)));
        assertEquals(600, service.feeFor(rule, observation(94), 300));
        assertEquals(1000, service.feeFor(rule, observation(120), 300));
    }

    @Test
    void fallsBackToBaseFeeWithoutMatchingTier() {
        when(tiers.findByRuleCodeOrderByOverFromAsc("SPEED_LIMIT_PRIVATE")).thenReturn(List.of());
        assertEquals(300, service.feeFor(rule, observation(94), 300));
    }
}

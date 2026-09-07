package com.quradar.fine;

import com.quradar.ingestion.Observation;
import com.quradar.rules.SpeedLimitRule;
import com.quradar.rules.ViolationRule;
import org.springframework.stereotype.Service;

/**
 * Tiered fine policy: how far over the limit decides the fee (DB-configured,
 * not hardcoded). Rules without tiers fall back to their configured base fee.
 */
@Service
public class FineCalculationService {

    private final FineTierRepository tiers;

    public FineCalculationService(FineTierRepository tiers) {
        this.tiers = tiers;
    }

    public int feeFor(ViolationRule rule, Observation observation, int baseFee) {
        int overBy = overBy(rule, observation);
        if (overBy <= 0) {
            return baseFee;
        }
        return tiers.findByRuleCodeOrderByOverFromAsc(rule.getRuleCode()).stream()
                .filter(tier -> tier.matches(overBy))
                .map(FineTier::getFee)
                .findFirst()
                .orElse(baseFee);
    }

    private int overBy(ViolationRule rule, Observation observation) {
        if (rule instanceof SpeedLimitRule speed) {
            return observation.getSpeed() - speed.getMaxSpeed();
        }
        return 0;
    }
}

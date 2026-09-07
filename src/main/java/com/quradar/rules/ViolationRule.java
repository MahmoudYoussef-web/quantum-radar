package com.quradar.rules;

import com.quradar.ingestion.Observation;
import com.quradar.violation.Violation;

/**
 * P3 contract: stable machine code, pure predicate, pure evaluator.
 * Implementations are plain POJOs (no Spring) so each rule is unit-testable alone.
 */
public interface ViolationRule {

    String getRuleCode();

    boolean matches(Observation observation);

    Violation evaluate(Observation observation);
}

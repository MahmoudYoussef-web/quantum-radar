package com.quradar.rules;

import com.quradar.ingestion.Observation;
import com.quradar.violation.Violation;

public interface ViolationRule {
    String getName();
    Violation evaluate(Observation observation);
}

package com.quradar.rules;

import com.quradar.fine.Fine;
import com.quradar.fine.FineEntity;
import com.quradar.fine.FineRepository;
import com.quradar.ingestion.Observation;
import com.quradar.ingestion.ObservationEntity;
import com.quradar.ingestion.ObservationRepository;
import com.quradar.violation.Violation;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rule engine orchestrator. Stateless except for the injected rule list;
 * all state lives in Postgres (observations, fines, violations).
 * NOTE: in-memory aggregation was deleted in P2 (no compat shim) and now
 * lives as DB queries in FineQueryService.
 */
@Service
public class QuRadar {

    private final List<ViolationRule> rules = new ArrayList<>();
    private final ObservationRepository observations;
    private final FineRepository fines;

    public QuRadar(ObservationRepository observations, FineRepository fines) {
        this.observations = observations;
        this.fines = fines;
    }

    public void addRule(ViolationRule rule) {
        rules.add(rule);
    }

    @Transactional
    public Fine processObservation(Observation observation) {
        List<Violation> violations = new ArrayList<>();

        for (ViolationRule rule : rules) {
            Violation violation = rule.evaluate(observation);
            if (violation != null) {
                violations.add(violation);
            }
        }

        if (violations.isEmpty()) {
            return null;
        }

        ObservationEntity observationEntity = observations.save(new ObservationEntity(
                observation.getPlateNumber(),
                observation.getDate(),
                observation.getCarType(),
                observation.getSpeed(),
                observation.isSeatbeltFastened()));

        int total = violations.stream().mapToInt(Violation::getFee).sum();
        FineEntity fineEntity = new FineEntity(observation.getPlateNumber(), total, observationEntity);
        for (Violation violation : violations) {
            fineEntity.addViolation(violation.getRuleName(), violation.getDescription(), violation.getFee());
        }
        fines.save(fineEntity);

        Fine fine = new Fine(observation.getPlateNumber(), violations);
        fine.print();

        return fine;
    }
}

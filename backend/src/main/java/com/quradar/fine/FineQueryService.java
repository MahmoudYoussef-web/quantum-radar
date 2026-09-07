package com.quradar.fine;

import com.quradar.violation.ViolationRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side aggregates. These DB GROUP BY queries are the P2 replacement for
 * the deleted in-memory QuRadar.getAllPossibleFines()/getAllViolatedRules().
 */
@Service
public class FineQueryService {

    private final FineRepository fines;
    private final ViolationRepository violations;

    public FineQueryService(FineRepository fines, ViolationRepository violations) {
        this.fines = fines;
        this.violations = violations;
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> getTotalFinesByPlate() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Object[] row : fines.sumTotalsByPlate()) {
            result.put((String) row[0], ((Number) row[1]).intValue());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getViolationCountsByRule() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : violations.countByRule()) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }
}

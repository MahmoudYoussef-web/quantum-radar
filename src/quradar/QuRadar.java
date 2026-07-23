package quradar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the main class of the system.
 * It takes an observation from the radar (plate, date, car type,
 * speed, seatbelt status) - the radar itself is assumed to use an
 * AI model to get this info.
 * It runs the rules on the observation, and if something is wrong
 * it creates a fine. You can add new rules without changing this class.
 */
public class QuRadar {

    private List<ViolationRule> rules;
    private List<Fine> fines;

    public QuRadar() {
        this.rules = new ArrayList<>();
        this.fines = new ArrayList<>();
    }

    public void addRule(ViolationRule rule) {
        rules.add(rule);
    }

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

        Fine fine = new Fine(observation.getPlateNumber(), violations);
        fines.add(fine);
        fine.print();

        return fine;
    }

    public Map<String, Integer> getAllPossibleFines() {
        Map<String, Integer> result = new LinkedHashMap<>();

        for (Fine fine : fines) {
            String plate = fine.getPlateNumber();

            if (result.containsKey(plate)) {
                int current = result.get(plate);
                result.put(plate, current + fine.getTotalAmount());
            } else {
                result.put(plate, fine.getTotalAmount());
            }
        }

        return result;
    }

    public Map<String, Integer> getAllViolatedRules() {
        Map<String, Integer> result = new LinkedHashMap<>();

        for (Fine fine : fines) {
            for (Violation violation : fine.getViolations()) {
                String ruleName = violation.getRuleName();

                if (result.containsKey(ruleName)) {
                    int current = result.get(ruleName);
                    result.put(ruleName, current + 1);
                } else {
                    result.put(ruleName, 1);
                }
            }
        }

        return result;
    }
}

package quradar;

public interface ViolationRule {
    String getName();
    Violation evaluate(Observation observation);
}

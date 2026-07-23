package quradar;

public class SeatbeltRule implements ViolationRule {

    private static final int FEE = 100;

    @Override
    public String getName() {
        return "SeatbeltRule";
    }

    @Override
    public Violation evaluate(Observation observation) {
        if (!observation.isSeatbeltFastened()) {
            return new Violation(getName(), "Seatbelt not fastned", FEE);
        }
        return null;
    }
}

package quradar;

public class SpeedLimitRule implements ViolationRule {

    private CarType carType;
    private int maxSpeed;
    private int fee;

    public SpeedLimitRule(CarType carType, int maxSpeed, int fee) {
        this.carType = carType;
        this.maxSpeed = maxSpeed;
        this.fee = fee;
    }

    @Override
    public String getName() {
        return "SpeedLimitRule(" + carType + ")";
    }

    @Override
    public Violation evaluate(Observation observation) {
        if (observation.getCarType() == carType && observation.getSpeed() > maxSpeed) {
            String description = "speed of " + observation.getSpeed() + " exceeded max allowed " + maxSpeed;
            return new Violation(getName(), description, fee);
        }
        return null;
    }
}

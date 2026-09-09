package com.quradar.violation;

public class Violation {

    private String ruleName;
    private String description;
    private int fee;
    private int points;
    private int ruleVersion;

    public Violation(String ruleName, String description, int fee) {
        this.ruleName = ruleName;
        this.description = description;
        this.fee = fee;
        this.points = 0;
        this.ruleVersion = 0;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getDescription() {
        return description;
    }

    public int getFee() {
        return fee;
    }

    public void setFee(int fee) {
        this.fee = fee;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(int ruleVersion) {
        this.ruleVersion = ruleVersion;
    }
}

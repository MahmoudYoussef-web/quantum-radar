package com.quradar.fine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fine_tiers")
public class FineTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(name = "over_from", nullable = false)
    private int overFrom;

    @Column(name = "over_to")
    private Integer overTo;

    @Column(nullable = false)
    private int fee;

    protected FineTier() {
    }

    public FineTier(String ruleCode, int overFrom, Integer overTo, int fee) {
        this.ruleCode = ruleCode;
        this.overFrom = overFrom;
        this.overTo = overTo;
        this.fee = fee;
    }

    public boolean matches(int overBy) {
        return overBy >= overFrom && (overTo == null || overBy <= overTo);
    }

    public Long getId() {
        return id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public int getOverFrom() {
        return overFrom;
    }

    public Integer getOverTo() {
        return overTo;
    }

    public int getFee() {
        return fee;
    }
}

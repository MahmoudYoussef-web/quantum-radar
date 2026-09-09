package com.quradar.violation;

import com.quradar.fine.FineEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "violations")
public class ViolationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fine_id", nullable = false)
    private FineEntity fine;

    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    @Column(nullable = false, length = 512)
    private String description;

    @Column(nullable = false)
    private int fee;

    @Column(nullable = false)
    private int points;

    @Column(name = "rule_version")
    private Integer ruleVersion;

    protected ViolationEntity() {
    }

    public ViolationEntity(FineEntity fine, String ruleName, String description, int fee,
                           int points, Integer ruleVersion) {
        this.fine = fine;
        this.ruleName = ruleName;
        this.description = description;
        this.fee = fee;
        this.points = points;
        this.ruleVersion = ruleVersion;
    }

    public Long getId() {
        return id;
    }

    public FineEntity getFine() {
        return fine;
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

    public int getPoints() {
        return points;
    }

    public Integer getRuleVersion() {
        return ruleVersion;
    }
}

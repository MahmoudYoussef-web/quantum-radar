package com.quradar.fine;

import com.quradar.ingestion.ObservationEntity;
import com.quradar.violation.ViolationEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fines")
public class FineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_number", nullable = false, length = 32)
    private String plateNumber;

    @Column(name = "total_amount", nullable = false)
    private int totalAmount;

    @ManyToOne
    @JoinColumn(name = "observation_id")
    private ObservationEntity observation;

    @OneToMany(mappedBy = "fine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ViolationEntity> violations = new ArrayList<>();

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FineEntity() {
    }

    public FineEntity(String plateNumber, int totalAmount, ObservationEntity observation) {
        this.plateNumber = plateNumber;
        this.totalAmount = totalAmount;
        this.observation = observation;
        this.createdAt = Instant.now();
    }

    public void addViolation(String ruleName, String description, int fee, int points) {
        violations.add(new ViolationEntity(this, ruleName, description, fee, points));
    }

    public Long getId() {
        return id;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public ObservationEntity getObservation() {
        return observation;
    }

    public List<ViolationEntity> getViolations() {
        return violations;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

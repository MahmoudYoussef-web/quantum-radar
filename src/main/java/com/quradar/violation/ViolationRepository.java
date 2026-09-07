package com.quradar.violation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ViolationRepository extends JpaRepository<ViolationEntity, Long> {

    /** Replaces the deleted in-memory getAllViolatedRules(): counts per rule computed in the DB. */
    @Query("SELECT v.ruleName, COUNT(v) FROM ViolationEntity v GROUP BY v.ruleName")
    List<Object[]> countByRule();
}

package com.quradar.violation;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ViolationRepository extends JpaRepository<ViolationEntity, Long> {

    /** Replaces the deleted in-memory getAllViolatedRules(): counts per rule computed in the DB. */
    @Query("SELECT v.ruleName, COUNT(v) FROM ViolationEntity v GROUP BY v.ruleName")
    List<Object[]> countByRule();

    Page<ViolationEntity> findByRuleName(String ruleName, Pageable pageable);

    @Query("SELECT v FROM ViolationEntity v WHERE v.fine.plateNumber = :plate")
    Page<ViolationEntity> findByPlate(String plate, Pageable pageable);

    @Query("SELECT v FROM ViolationEntity v WHERE v.fine.plateNumber IN :plates")
    Page<ViolationEntity> findByPlates(List<String> plates, Pageable pageable);

    @Query("SELECT v FROM ViolationEntity v WHERE v.fine.plateNumber = :plate AND v.ruleName = :rule")
    Page<ViolationEntity> findByPlateAndRule(String plate, String rule, Pageable pageable);

    @Query("SELECT v FROM ViolationEntity v WHERE v.fine.plateNumber IN :plates AND v.ruleName = :rule")
    Page<ViolationEntity> findByPlatesAndRule(List<String> plates, String rule, Pageable pageable);
}

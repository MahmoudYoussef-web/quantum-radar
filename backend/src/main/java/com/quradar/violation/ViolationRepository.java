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

    @Query("SELECT v.fine.observation.observedAt, COUNT(v) FROM ViolationEntity v "
            + "WHERE v.fine.observation.observedAt >= :since "
            + "GROUP BY v.fine.observation.observedAt "
            + "ORDER BY v.fine.observation.observedAt")
    List<Object[]> countByDay(java.time.LocalDate since);

    Page<ViolationEntity> findByRuleName(String ruleName, Pageable pageable);

    @Query("SELECT v FROM ViolationEntity v WHERE v.fine.plateNumber = :plate")
    Page<ViolationEntity> findByPlate(String plate, Pageable pageable);

    @Query("SELECT v FROM ViolationEntity v WHERE v.fine.plateNumber IN :plates")
    Page<ViolationEntity> findByPlates(List<String> plates, Pageable pageable);

    @Query("SELECT v FROM ViolationEntity v WHERE v.fine.plateNumber = :plate AND v.ruleName = :rule")
    Page<ViolationEntity> findByPlateAndRule(String plate, String rule, Pageable pageable);

    @Query("SELECT v FROM ViolationEntity v WHERE v.fine.plateNumber IN :plates AND v.ruleName = :rule")
    Page<ViolationEntity> findByPlatesAndRule(List<String> plates, String rule, Pageable pageable);

    @Query("SELECT v FROM ViolationEntity v "
            + "WHERE (:rule IS NULL OR v.ruleName = :rule) "
            + "AND (:plate IS NULL OR v.fine.plateNumber = :plate) "
            + "AND (:device IS NULL OR v.fine.observation.device.deviceCode = :device) "
            + "AND v.fine.createdAt >= COALESCE(:from, v.fine.createdAt) "
            + "AND v.fine.createdAt <= COALESCE(:to, v.fine.createdAt) "
            + "AND (:minFee IS NULL OR v.fee >= :minFee) "
            + "AND (:maxFee IS NULL OR v.fee <= :maxFee)")
    Page<ViolationEntity> search(String rule, String plate, String device,
                                 java.time.Instant from, java.time.Instant to,
                                 Integer minFee, Integer maxFee, Pageable pageable);

    @Query("SELECT v FROM ViolationEntity v WHERE v.fine.plateNumber IN :plates "
            + "AND (:rule IS NULL OR v.ruleName = :rule) "
            + "AND (:device IS NULL OR v.fine.observation.device.deviceCode = :device) "
            + "AND v.fine.createdAt >= COALESCE(:from, v.fine.createdAt) "
            + "AND v.fine.createdAt <= COALESCE(:to, v.fine.createdAt) "
            + "AND (:minFee IS NULL OR v.fee >= :minFee) "
            + "AND (:maxFee IS NULL OR v.fee <= :maxFee)")
    Page<ViolationEntity> searchForPlates(List<String> plates, String rule, String device,
                                         java.time.Instant from, java.time.Instant to,
                                         Integer minFee, Integer maxFee, Pageable pageable);
}

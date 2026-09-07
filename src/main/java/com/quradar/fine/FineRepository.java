package com.quradar.fine;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FineRepository extends JpaRepository<FineEntity, Long> {

    /** Replaces the deleted in-memory getAllPossibleFines(): totals per plate computed in the DB. */
    @Query("SELECT f.plateNumber, SUM(f.totalAmount) FROM FineEntity f GROUP BY f.plateNumber")
    List<Object[]> sumTotalsByPlate();
}

package com.quradar.fine;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FineTierRepository extends JpaRepository<FineTier, Long> {

    List<FineTier> findByRuleCodeOrderByOverFromAsc(String ruleCode);
}

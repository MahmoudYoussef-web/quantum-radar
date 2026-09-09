package com.quradar.rules;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleVersionRepository extends JpaRepository<RuleVersion, Long> {

    List<RuleVersion> findByRuleCodeOrderByVersionDesc(String ruleCode);

    Optional<RuleVersion> findTopByRuleCodeAndEffectiveFromLessThanEqualOrderByVersionDesc(
            String ruleCode, Instant now);
}

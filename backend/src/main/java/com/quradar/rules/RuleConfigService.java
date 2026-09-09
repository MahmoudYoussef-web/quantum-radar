package com.quradar.rules;

import com.quradar.common.CarType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds live rule objects from the latest EFFECTIVE version per code — never
 * from the editable config row directly. Every tuning snapshots a new version,
 * and each violation pins the version it was judged under, so old fines never
 * move when a rule changes. The config row stays the admin editing surface.
 */
@Service
public class RuleConfigService {

    private final RuleConfigRepository repository;
    private final RuleVersionRepository versions;

    public RuleConfigService(RuleConfigRepository repository, RuleVersionRepository versions) {
        this.repository = repository;
        this.versions = versions;
    }

    @Transactional(readOnly = true)
    public List<RuleConfig> listAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public RuleConfig getRequired(String code) {
        return repository.findById(code).orElseThrow(() -> new RuleNotFoundException(code));
    }

    @Transactional(readOnly = true)
    public List<RuleVersion> listVersions(String code) {
        getRequired(code);
        return versions.findByRuleCodeOrderByVersionDesc(code);
    }

    @Transactional
    public RuleConfig update(String code, RuleConfigUpdateRequest request) {
        RuleConfig config = getRequired(code);
        if (request.enabled() != null) {
            config.setEnabled(request.enabled());
        }
        if (request.fee() != null) {
            config.setFee(request.fee());
        }
        if (request.penaltyPoints() != null) {
            config.setPenaltyPoints(request.penaltyPoints());
        }
        if (request.maxSpeed() != null) {
            config.setMaxSpeed(request.maxSpeed());
        }
        int next = versions.findByRuleCodeOrderByVersionDesc(code).stream()
                .mapToInt(RuleVersion::getVersion).max().orElse(0) + 1;
        versions.save(new RuleVersion(code, next, config, Instant.now()));
        return config;
    }

    @Transactional(readOnly = true)
    public int pointsFor(String code) {
        return current(code).map(RuleVersion::getPenaltyPoints)
                .orElseGet(() -> repository.findById(code)
                        .map(RuleConfig::getPenaltyPoints).orElse(0));
    }

    @Transactional(readOnly = true)
    public int versionFor(String code) {
        return current(code).map(RuleVersion::getVersion).orElse(0);
    }

    @Transactional(readOnly = true)
    public List<ViolationRule> buildEnabledRules() {
        List<ViolationRule> rules = new ArrayList<>();
        for (RuleConfig config : repository.findAll()) {
            current(config.getCode()).ifPresent(live -> {
                if (!live.isEnabled()) {
                    return;
                }
                ViolationRule rule = toRule(live);
                if (rule != null) {
                    rules.add(rule);
                }
            });
        }
        return rules;
    }

    private java.util.Optional<RuleVersion> current(String code) {
        return versions.findTopByRuleCodeAndEffectiveFromLessThanEqualOrderByVersionDesc(
                code, Instant.now());
    }

    private ViolationRule toRule(RuleVersion live) {
        String code = live.getRuleCode();
        if ("SEATBELT".equals(code)) {
            return new SeatbeltRule(code, live.getFee());
        }
        if (code.startsWith("SPEED_LIMIT_")) {
            try {
                CarType carType = CarType.valueOf(code.substring("SPEED_LIMIT_".length()));
                if (live.getMaxSpeed() != null) {
                    return new SpeedLimitRule(code, carType, live.getMaxSpeed(), live.getFee());
                }
            } catch (IllegalArgumentException ex) {
                return null;
            }
            return null;
        }
        if ("RESTRICTED_ZONE".equals(code)) {
            if (live.getZoneMinLat() != null && live.getZoneMaxLat() != null
                    && live.getZoneMinLon() != null && live.getZoneMaxLon() != null) {
                return new RestrictedZoneRule(code, live.getZoneMinLat(), live.getZoneMaxLat(),
                        live.getZoneMinLon(), live.getZoneMaxLon(), live.getFee());
            }
            return null;
        }
        if ("RED_LIGHT".equals(code)) {
            return new RedLightRule(code, live.getFee());
        }
        return null;
    }
}

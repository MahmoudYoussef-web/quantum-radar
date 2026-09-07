package com.quradar.rules;

import com.quradar.common.CarType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds live rule objects from DB configuration. ADMIN edits RuleConfig rows;
 * the engine picks them up on the next observation (no redeploy, no hardcoding).
 */
@Service
public class RuleConfigService {

    private final RuleConfigRepository repository;

    public RuleConfigService(RuleConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<RuleConfig> listAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public RuleConfig getRequired(String code) {
        return repository.findById(code).orElseThrow(() -> new RuleNotFoundException(code));
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
        return config;
    }

    @Transactional(readOnly = true)
    public List<ViolationRule> buildEnabledRules() {
        List<ViolationRule> rules = new ArrayList<>();
        for (RuleConfig config : repository.findAll()) {
            if (!config.isEnabled()) {
                continue;
            }
            ViolationRule rule = toRule(config);
            if (rule != null) {
                rules.add(rule);
            }
        }
        return rules;
    }

    private ViolationRule toRule(RuleConfig config) {
        String code = config.getCode();
        if ("SEATBELT".equals(code)) {
            return new SeatbeltRule(code, config.getFee());
        }
        if (code.startsWith("SPEED_LIMIT_")) {
            try {
                CarType carType = CarType.valueOf(code.substring("SPEED_LIMIT_".length()));
                if (config.getMaxSpeed() != null) {
                    return new SpeedLimitRule(code, carType, config.getMaxSpeed(), config.getFee());
                }
            } catch (IllegalArgumentException ex) {
                return null;
            }
            return null;
        }
        if ("RESTRICTED_ZONE".equals(code)) {
            if (config.getZoneMinLat() != null && config.getZoneMaxLat() != null
                    && config.getZoneMinLon() != null && config.getZoneMaxLon() != null) {
                return new RestrictedZoneRule(code, config.getZoneMinLat(), config.getZoneMaxLat(),
                        config.getZoneMinLon(), config.getZoneMaxLon(), config.getFee());
            }
            return null;
        }
        if ("RED_LIGHT".equals(code)) {
            return new RedLightRule(code, config.getFee());
        }
        return null;
    }
}

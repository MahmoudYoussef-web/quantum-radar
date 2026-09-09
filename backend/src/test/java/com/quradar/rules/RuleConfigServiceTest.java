package com.quradar.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleConfigServiceTest {

    @Mock
    private RuleConfigRepository repository;

    @Mock
    private RuleVersionRepository versions;

    @InjectMocks
    private RuleConfigService service;

    private static RuleConfig config(String code, boolean enabled, int fee, Integer maxSpeed,
                                     Double minLat, Double maxLat, Double minLon, Double maxLon,
                                     int points) throws Exception {
        RuleConfig config = new RuleConfig();
        set(config, "code", code);
        set(config, "displayName", code);
        set(config, "enabled", enabled);
        set(config, "fee", fee);
        set(config, "penaltyPoints", points);
        set(config, "maxSpeed", maxSpeed);
        set(config, "zoneMinLat", minLat);
        set(config, "zoneMaxLat", maxLat);
        set(config, "zoneMinLon", minLon);
        set(config, "zoneMaxLon", maxLon);
        return config;
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void buildsOnlyValidEnabledRules() throws Exception {
        List<RuleConfig> configs = List.of(
                config("SEATBELT", true, 100, null, null, null, null, null, 1),
                config("SPEED_LIMIT_PRIVATE", true, 300, 80, null, null, null, null, 2),
                config("SPEED_LIMIT_TRUCK", true, 300, null, null, null, null, null, 2),
                config("RESTRICTED_ZONE", true, 500, null, null, null, null, null, 2),
                config("RED_LIGHT", false, 500, null, null, null, null, null, 3),
                config("MYSTERY_RULE", true, 10, null, null, null, null, null, 0));
        when(repository.findAll()).thenReturn(configs);
        when(versions.findTopByRuleCodeAndEffectiveFromLessThanEqualOrderByVersionDesc(
                org.mockito.ArgumentMatchers.anyString(), any(Instant.class)))
                .thenAnswer(invocation -> {
                    String code = invocation.getArgument(0);
                    return configs.stream().filter(c -> c.getCode().equals(code)).findFirst()
                            .map(c -> new RuleVersion(code, 1, c, Instant.now()));
                });

        List<String> codes = service.buildEnabledRules().stream()
                .map(ViolationRule::getRuleCode)
                .sorted()
                .collect(Collectors.toList());

        assertEquals(List.of("SEATBELT", "SPEED_LIMIT_PRIVATE"), codes);
    }

    @Test
    void pointsForFallsBackToZero() {
        assertEquals(0, service.pointsFor("NOPE"));
    }
}

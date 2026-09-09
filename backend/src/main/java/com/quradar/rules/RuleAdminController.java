package com.quradar.rules;

import com.quradar.audit.AuditService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rule configuration is ADMIN-only. */
@RestController
@RequestMapping("/api/v1/rules")
@PreAuthorize("hasRole('ADMIN')")
public class RuleAdminController {

    private final RuleConfigService service;
    private final AuditService audit;

    public RuleAdminController(RuleConfigService service, AuditService audit) {
        this.service = service;
        this.audit = audit;
    }

    @GetMapping
    public List<RuleConfigResponse> list() {
        return service.listAll().stream().map(RuleConfigResponse::from).toList();
    }

    @GetMapping("/{code}")
    public RuleConfigResponse get(@PathVariable String code) {
        return RuleConfigResponse.from(service.getRequired(code));
    }

    @GetMapping("/{code}/versions")
    public List<RuleVersionResponse> versions(@PathVariable String code) {
        return service.listVersions(code).stream().map(RuleVersionResponse::from).toList();
    }

    @PatchMapping("/{code}")
    public RuleConfigResponse update(@PathVariable String code,
                                     @Valid @RequestBody RuleConfigUpdateRequest request) {        RuleConfig before = service.getRequired(code);
        RuleConfigResponse updated = RuleConfigResponse.from(service.update(code, request));
        audit.record("UPDATED_RULE", "RULE", code, diff(before, updated));
        return updated;
    }

    private static java.util.Map<String, Object> diff(RuleConfig before, RuleConfigResponse after) {
        java.util.Map<String, Object> changes = new LinkedHashMap<>();
        putIfChanged(changes, "enabled", before.isEnabled(), after.enabled());
        putIfChanged(changes, "fee", before.getFee(), after.fee());
        putIfChanged(changes, "penaltyPoints", before.getPenaltyPoints(), after.penaltyPoints());
        putIfChanged(changes, "maxSpeed", before.getMaxSpeed(), after.maxSpeed());
        return changes;
    }

    private static void putIfChanged(java.util.Map<String, Object> changes, String field,
                                     Object from, Object to) {
        if ((from == null && to != null) || (from != null && !from.equals(to))) {
            changes.put(field, java.util.Map.of("from", String.valueOf(from), "to", String.valueOf(to)));
        }
    }
}

package com.quradar.rules;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** NOTE: no auth until P6 — local-only. P6 restricts this to ADMIN. */
@RestController
@RequestMapping("/api/v1/rules")
public class RuleAdminController {

    private final RuleConfigService service;

    public RuleAdminController(RuleConfigService service) {
        this.service = service;
    }

    @GetMapping
    public List<RuleConfigResponse> list() {
        return service.listAll().stream().map(RuleConfigResponse::from).toList();
    }

    @GetMapping("/{code}")
    public RuleConfigResponse get(@PathVariable String code) {
        return RuleConfigResponse.from(service.getRequired(code));
    }

    @PatchMapping("/{code}")
    public RuleConfigResponse update(@PathVariable String code,
                                     @Valid @RequestBody RuleConfigUpdateRequest request) {
        return RuleConfigResponse.from(service.update(code, request));
    }
}

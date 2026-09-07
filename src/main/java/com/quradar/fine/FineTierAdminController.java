package com.quradar.fine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Fine-tier policy is ADMIN-only. */
@RestController
@RequestMapping("/api/v1/fine-tiers")
@PreAuthorize("hasRole('ADMIN')")
public class FineTierAdminController {

    private final FineTierRepository tiers;

    public FineTierAdminController(FineTierRepository tiers) {
        this.tiers = tiers;
    }

    public record FineTierResponse(Long id, String ruleCode, int overFrom, Integer overTo, int fee) {
        static FineTierResponse from(FineTier tier) {
            return new FineTierResponse(tier.getId(), tier.getRuleCode(), tier.getOverFrom(),
                    tier.getOverTo(), tier.getFee());
        }
    }

    public record FineTierCreateRequest(@NotBlank String ruleCode, @Min(1) int overFrom,
            Integer overTo, @NotNull @Min(0) Integer fee) {
    }

    @GetMapping
    public List<FineTierResponse> list() {
        return tiers.findAll().stream().map(FineTierResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<FineTierResponse> create(@Valid @RequestBody FineTierCreateRequest request) {
        FineTier saved = tiers.save(new FineTier(request.ruleCode(), request.overFrom(),
                request.overTo(), request.fee()));
        return ResponseEntity.status(HttpStatus.CREATED).body(FineTierResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!tiers.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        tiers.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

package com.quradar.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only audit trail. ADMIN and OFFICER may inspect who changed what. */
@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
public class AuditController {

    private final AuditRepository repository;

    public AuditController(AuditRepository repository) {
        this.repository = repository;
    }

    public record AuditResponse(Long id, java.time.Instant occurredAt, String actor,
            String action, String entityType, String entityId, String details, String ipAddress) {
        static AuditResponse from(AuditEntry entry) {
            return new AuditResponse(entry.getId(), entry.getOccurredAt(), entry.getActor(),
                    entry.getAction(), entry.getEntityType(), entry.getEntityId(),
                    entry.getDetails(), entry.getIpAddress());
        }
    }

    @GetMapping
    public Page<AuditResponse> list(Pageable pageable) {
        return repository.findAllByOrderByOccurredAtDesc(pageable).map(AuditResponse::from);
    }
}

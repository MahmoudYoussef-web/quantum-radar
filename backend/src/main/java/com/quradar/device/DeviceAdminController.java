package com.quradar.device;

import com.quradar.audit.AuditService;
import com.quradar.security.SecuritySupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Device registry is ADMIN-only. */
@RestController
@RequestMapping("/api/v1/devices")
@PreAuthorize("hasRole('ADMIN')")
public class DeviceAdminController {

    private final DeviceRepository repository;
    private final DeviceHealthService health;
    private final AuditService audit;
    private final SecuritySupport security;

    public DeviceAdminController(DeviceRepository repository, DeviceHealthService health,
                                 AuditService audit, SecuritySupport security) {
        this.repository = repository;
        this.health = health;
        this.audit = audit;
        this.security = security;
    }

    public record DeviceResponse(String deviceCode, String name, boolean active,
            DeviceHealth health, Instant lastSeenAt) {
        static DeviceResponse from(DeviceEntity device, DeviceHealthService health) {
            return new DeviceResponse(device.getDeviceCode(), device.getName(), device.isActive(),
                    health.healthOf(device), device.getLastSeenAt());
        }
    }

    public record DeviceCreateRequest(@NotBlank String deviceCode, @NotBlank String name) {
    }

    public record DeviceUpdateRequest(Boolean active) {
    }

    @GetMapping
    public List<DeviceResponse> list() {
        return repository.findAll().stream().map(d -> DeviceResponse.from(d, health)).toList();
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public DeviceHealthService.DeviceDetail detail(@PathVariable String code) {
        DeviceEntity device = repository.findByDeviceCode(code)
                .orElseThrow(() -> new DeviceNotFoundException(code));
        return health.detail(device);
    }

    public record HeartbeatRequest(String firmwareVersion) {
    }

    @PostMapping("/{code}/heartbeat")
    @Transactional
    @PreAuthorize("hasAnyRole('DEVICE','ADMIN')")
    public DeviceResponse heartbeat(@PathVariable String code,
                                    @RequestBody(required = false) HeartbeatRequest request,
                                    HttpServletRequest http) {
        security.requireDeviceOwner(code);
        DeviceEntity device = repository.findByDeviceCode(code)
                .orElseThrow(() -> new DeviceNotFoundException(code));
        device.seen(request != null ? request.firmwareVersion() : null, http.getRemoteAddr());
        return DeviceResponse.from(device, health);
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> create(@Valid @RequestBody DeviceCreateRequest request) {
        DeviceEntity saved = repository.save(
                new DeviceEntity(request.deviceCode(), request.name(), true));
        audit.record("CREATED_DEVICE", "DEVICE", saved.getDeviceCode(),
                Map.of("name", saved.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(DeviceResponse.from(saved, health));
    }

    @PatchMapping("/{code}")
    @Transactional
    public DeviceResponse update(@PathVariable String code, @RequestBody DeviceUpdateRequest request) {
        DeviceEntity device = repository.findByDeviceCode(code)
                .orElseThrow(() -> new DeviceNotFoundException(code));
        if (request.active() != null && request.active() != device.isActive()) {
            boolean from = device.isActive();
            device.setActive(request.active());
            audit.record(request.active() ? "ACTIVATED_DEVICE" : "DEACTIVATED_DEVICE", "DEVICE",
                    code, Map.of("active", Map.of("from", from, "to", request.active())));
        }
        return DeviceResponse.from(device, health);
    }
}

package com.quradar.device;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public DeviceAdminController(DeviceRepository repository) {
        this.repository = repository;
    }

    public record DeviceResponse(String deviceCode, String name, boolean active) {
        static DeviceResponse from(DeviceEntity device) {
            return new DeviceResponse(device.getDeviceCode(), device.getName(), device.isActive());
        }
    }

    public record DeviceCreateRequest(@NotBlank String deviceCode, @NotBlank String name) {
    }

    public record DeviceUpdateRequest(Boolean active) {
    }

    @GetMapping
    public List<DeviceResponse> list() {
        return repository.findAll().stream().map(DeviceResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> create(@Valid @RequestBody DeviceCreateRequest request) {
        DeviceEntity saved = repository.save(
                new DeviceEntity(request.deviceCode(), request.name(), true));
        return ResponseEntity.status(HttpStatus.CREATED).body(DeviceResponse.from(saved));
    }

    @PatchMapping("/{code}")
    public DeviceResponse update(@PathVariable String code, @RequestBody DeviceUpdateRequest request) {
        DeviceEntity device = repository.findByDeviceCode(code)
                .orElseThrow(() -> new DeviceNotFoundException(code));
        if (request.active() != null) {
            device.setActive(request.active());
        }
        return DeviceResponse.from(device);
    }
}

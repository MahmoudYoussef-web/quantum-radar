package com.quradar.security;

import com.quradar.audit.AuditService;
import com.quradar.device.DeviceEntity;
import com.quradar.device.DeviceNotFoundException;
import com.quradar.device.DeviceRepository;
import com.quradar.driver.Driver;
import com.quradar.driver.DriverNotFoundException;
import com.quradar.driver.DriverRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository users;
    private final DriverRepository drivers;
    private final DeviceRepository devices;
    private final PasswordEncoder passwords;
    private final AuditService audit;

    public AdminController(UserRepository users, DriverRepository drivers,
                           DeviceRepository devices, PasswordEncoder passwords,
                           AuditService audit) {
        this.users = users;
        this.drivers = drivers;
        this.devices = devices;
        this.passwords = passwords;
        this.audit = audit;
    }

    public record UserCreateRequest(@NotBlank String username, @NotBlank @Size(min = 8) String password,
            @NotNull Role role, String driverLicenseNo, String deviceCode) {
    }

    public record UserResponse(String username, Role role, boolean enabled,
            String driverLicenseNo, String deviceCode) {
        static UserResponse from(UserEntity user) {
            return new UserResponse(user.getUsername(), user.getRole(), user.isEnabled(),
                    user.getDriver() != null ? user.getDriver().getLicenseNo() : null,
                    user.getDevice() != null ? user.getDevice().getDeviceCode() : null);
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAll().stream().map(UserResponse::from).toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        if (users.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Driver driver = null;
        DeviceEntity device = null;
        if (request.role() == Role.CITIZEN) {
            if (request.driverLicenseNo() == null) {
                throw new IllegalArgumentException("CITIZEN user requires driverLicenseNo");
            }
            driver = drivers.findByLicenseNo(request.driverLicenseNo())
                    .orElseThrow(() -> new DriverNotFoundException(request.driverLicenseNo()));
        }
        if (request.role() == Role.DEVICE) {
            if (request.deviceCode() == null) {
                throw new IllegalArgumentException("DEVICE user requires deviceCode");
            }
            device = devices.findByDeviceCode(request.deviceCode())
                    .orElseThrow(() -> new DeviceNotFoundException(request.deviceCode()));
        }
        UserEntity saved = users.save(new UserEntity(request.username(),
                passwords.encode(request.password()), request.role(), true, driver, device));
        audit.record("CREATED_USER", "USER", saved.getUsername(),
                java.util.Map.of("role", saved.getRole().name()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(saved));
    }
}

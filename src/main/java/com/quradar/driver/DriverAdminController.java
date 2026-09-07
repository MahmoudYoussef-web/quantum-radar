package com.quradar.driver;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** NOTE: no JWT until P6 — local-only. P6 restricts this to ADMIN/OFFICER. */
@RestController
@RequestMapping("/api/v1/drivers")
public class DriverAdminController {

    private final DriverRepository drivers;
    private final LicenseRepository licenses;

    public DriverAdminController(DriverRepository drivers, LicenseRepository licenses) {
        this.drivers = drivers;
        this.licenses = licenses;
    }

    public record DriverCreateRequest(@NotBlank String name, @NotBlank String licenseNo,
            @NotNull LicenseStatus status, @NotNull LocalDate issuedAt, @NotNull LocalDate expiresAt) {
    }

    public record DriverResponse(String name, String licenseNo, int penaltyPoints, Long version,
            LicenseStatus licenseStatus) {
    }

    @PostMapping
    @Transactional
    public ResponseEntity<DriverResponse> create(
            @Valid @RequestBody DriverCreateRequest request) {
        Driver driver = drivers.save(new Driver(request.name(), request.licenseNo()));
        licenses.save(new License(driver, request.status(), request.issuedAt(), request.expiresAt()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(driver));
    }

    @GetMapping("/{licenseNo}")
    @Transactional(readOnly = true)
    public DriverResponse get(@PathVariable String licenseNo) {
        Driver driver = drivers.findByLicenseNo(licenseNo)
                .orElseThrow(() -> new DriverNotFoundException(licenseNo));
        return toResponse(driver);
    }

    private DriverResponse toResponse(Driver driver) {
        LicenseStatus status = licenses.findByDriverId(driver.getId())
                .map(License::getStatus).orElse(null);
        return new DriverResponse(driver.getName(), driver.getLicenseNo(),
                driver.getPenaltyPoints(), driver.getVersion(), status);
    }
}

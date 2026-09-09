package com.quradar.driver;

import com.quradar.fine.FineRepository;
import com.quradar.security.SecuritySupport;
import com.quradar.vehicle.Vehicle;
import com.quradar.vehicle.VehicleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Driver registration is ADMIN-only; lookups allow OFFICER and own-record CITIZEN. */
@RestController
@RequestMapping("/api/v1/drivers")
public class DriverAdminController {

    private final DriverRepository drivers;
    private final LicenseRepository licenses;
    private final VehicleRepository vehicles;
    private final FineRepository fines;
    private final SecuritySupport security;

    public DriverAdminController(DriverRepository drivers, LicenseRepository licenses,
                                 VehicleRepository vehicles, FineRepository fines,
                                 SecuritySupport security) {
        this.drivers = drivers;
        this.licenses = licenses;
        this.vehicles = vehicles;
        this.fines = fines;
        this.security = security;
    }

    public record DriverCreateRequest(@NotBlank String name, @NotBlank String licenseNo,
            @NotNull LicenseStatus status, @NotNull LocalDate issuedAt, @NotNull LocalDate expiresAt) {
    }

    public record DriverResponse(String name, String licenseNo, int penaltyPoints, Long version,
            LicenseStatus licenseStatus) {
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DriverResponse> create(
            @Valid @RequestBody DriverCreateRequest request) {
        Driver driver = drivers.save(new Driver(request.name(), request.licenseNo()));
        licenses.save(new License(driver, request.status(), request.issuedAt(), request.expiresAt()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(driver));
    }

    @GetMapping("/{licenseNo}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','CITIZEN')")
    public DriverResponse get(@PathVariable String licenseNo) {
        security.requireDriverOwner(licenseNo);
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

    public record VehicleEntry(String plate, String carType) {
    }

    public record EnforcementSummary(DriverResponse driver, List<VehicleEntry> vehicles,
            long totalViolations, long totalFines, int totalFineAmount) {
    }

    @GetMapping("/{licenseNo}/summary")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','CITIZEN')")
    public EnforcementSummary summary(@PathVariable String licenseNo) {
        security.requireDriverOwner(licenseNo);
        Driver driver = drivers.findByLicenseNo(licenseNo)
                .orElseThrow(() -> new DriverNotFoundException(licenseNo));
        List<Vehicle> owned = vehicles.findByOwnerId(driver.getId());
        long violationCount = 0;
        long fineCount = 0;
        int fineAmount = 0;
        for (Vehicle vehicle : owned) {
            var history = fines.findByPlateNumberOrderByCreatedAtDesc(vehicle.getPlate());
            fineCount += history.size();
            fineAmount += history.stream().mapToInt(f -> f.getTotalAmount()).sum();
            violationCount += history.stream().mapToInt(f -> f.getViolations().size()).sum();
        }
        List<VehicleEntry> entries = owned.stream()
                .map(v -> new VehicleEntry(v.getPlate(), v.getCarType().name())).toList();
        return new EnforcementSummary(toResponse(driver), entries, violationCount, fineCount,
                fineAmount);
    }
}

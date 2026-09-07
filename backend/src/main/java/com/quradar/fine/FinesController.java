package com.quradar.fine;

import com.quradar.driver.Driver;
import com.quradar.driver.DriverRepository;
import com.quradar.security.Role;
import com.quradar.security.SecuritySupport;
import com.quradar.vehicle.VehicleRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Paginated fine queries. CITIZEN is scoped to their own plates server-side. */
@RestController
@RequestMapping("/api/v1/fines")
@PreAuthorize("hasAnyRole('ADMIN','OFFICER','CITIZEN')")
public class FinesController {

    private final FineRepository fines;
    private final DriverRepository drivers;
    private final VehicleRepository vehicles;
    private final SecuritySupport security;

    public FinesController(FineRepository fines, DriverRepository drivers,
                           VehicleRepository vehicles, SecuritySupport security) {
        this.fines = fines;
        this.drivers = drivers;
        this.vehicles = vehicles;
        this.security = security;
    }

    public record FineSummary(Long id, String plateNumber, int totalAmount, int violations,
            Long version, Instant createdAt) {
        static FineSummary from(FineEntity fine) {
            return new FineSummary(fine.getId(), fine.getPlateNumber(), fine.getTotalAmount(),
                    fine.getViolations().size(), fine.getVersion(), fine.getCreatedAt());
        }
    }

    @GetMapping
    public Page<FineSummary> list(@RequestParam(required = false) String plate, Pageable pageable) {
        var principal = security.current();
        if (principal.role() == Role.CITIZEN) {
            Driver driver = drivers.findByLicenseNo(principal.driverLicenseNo()).orElseThrow();
            List<String> ownPlates =
                    vehicles.findByOwnerId(driver.getId()).stream().map(v -> v.getPlate()).toList();
            if (plate != null && !ownPlates.contains(plate)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Citizens may only query their own plates");
            }
            if (plate != null) {
                return fines.findByPlateNumber(plate, pageable).map(FineSummary::from);
            }
            if (ownPlates.isEmpty()) {
                return Page.empty(pageable);
            }
            return fines.findByPlateNumberIn(ownPlates, pageable).map(FineSummary::from);
        }
        if (plate != null) {
            return fines.findByPlateNumber(plate, pageable).map(FineSummary::from);
        }
        return fines.findAll(pageable).map(FineSummary::from);
    }

    @GetMapping("/{id}")
    public FineSummary get(@PathVariable Long id) {
        FineEntity fine = fines.findById(id)
                .orElseThrow(() -> new FineNotFoundException(id));
        var principal = security.current();
        if (principal.role() == Role.CITIZEN) {
            Driver driver = drivers.findByLicenseNo(principal.driverLicenseNo()).orElseThrow();
            boolean own = vehicles.findByOwnerId(driver.getId()).stream()
                    .anyMatch(v -> v.getPlate().equals(fine.getPlateNumber()));
            if (!own) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Citizens may only view their own fines");
            }
        }
        return FineSummary.from(fine);
    }
}

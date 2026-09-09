package com.quradar.violation;

import com.quradar.driver.Driver;
import com.quradar.driver.DriverRepository;
import com.quradar.security.Role;
import com.quradar.security.SecuritySupport;
import com.quradar.vehicle.VehicleRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Filtered violation search with the same CITIZEN plate scoping as fines. */
@RestController
@RequestMapping("/api/v1/violations")
@PreAuthorize("hasAnyRole('ADMIN','OFFICER','CITIZEN')")
@Transactional(readOnly = true)
public class ViolationsController {

    private final ViolationRepository violations;
    private final DriverRepository drivers;
    private final VehicleRepository vehicles;
    private final SecuritySupport security;

    public ViolationsController(ViolationRepository violations, DriverRepository drivers,
                                VehicleRepository vehicles, SecuritySupport security) {
        this.violations = violations;
        this.drivers = drivers;
        this.vehicles = vehicles;
        this.security = security;
    }

    public record ViolationSummary(Long id, Long fineId, String plateNumber, String ruleName,
            String description, int fee, int points, String deviceCode, Instant recordedAt,
            Integer ruleVersion) {
        static ViolationSummary from(ViolationEntity violation) {
            var observation = violation.getFine().getObservation();
            return new ViolationSummary(violation.getId(), violation.getFine().getId(),
                    violation.getFine().getPlateNumber(), violation.getRuleName(),
                    violation.getDescription(), violation.getFee(), violation.getPoints(),
                    observation != null && observation.getDevice() != null
                            ? observation.getDevice().getDeviceCode() : null,
                    violation.getFine().getCreatedAt(), violation.getRuleVersion());
        }
    }

    @GetMapping
    public Page<ViolationSummary> list(@RequestParam(required = false) String rule,
                                       @RequestParam(required = false) String plate,
                                       @RequestParam(required = false) String device,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                       @RequestParam(required = false) Integer minFee,
                                       @RequestParam(required = false) Integer maxFee,
                                       Pageable pageable) {
        var principal = security.current();
        if (principal.role() == Role.CITIZEN) {
            Driver driver = drivers.findByLicenseNo(principal.driverLicenseNo()).orElseThrow();
            List<String> plates = vehicles.findByOwnerId(driver.getId()).stream()
                    .map(v -> v.getPlate()).toList();
            if (plate != null && !plates.contains(plate)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Citizens may only query their own plates");
            }
            if (plates.isEmpty()) {
                return Page.empty(pageable);
            }
            return violations.searchForPlates(plates, rule, device, from, to, minFee, maxFee,
                    pageable).map(ViolationSummary::from);
        }
        return violations.search(rule, plate, device, from, to, minFee, maxFee, pageable)
                .map(ViolationSummary::from);
    }

    public record DayCount(String date, long count) {
    }

    public record RuleCount(String rule, long count) {
    }

    @GetMapping("/stats/daily")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public java.util.List<DayCount> daily(@RequestParam(defaultValue = "14") int days) {
        int window = Math.min(Math.max(days, 1), 90);
        java.time.LocalDate since = java.time.LocalDate.now().minusDays(window - 1);
        return violations.countByDay(since).stream()
                .map(row -> new DayCount(row[0].toString(), (Long) row[1]))
                .toList();
    }

    @GetMapping("/stats/by-rule")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public java.util.List<RuleCount> byRule() {
        return violations.countByRule().stream()
                .map(row -> new RuleCount((String) row[0], (Long) row[1]))
                .toList();
    }
}

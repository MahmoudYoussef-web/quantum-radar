package com.quradar.vehicle;

import com.quradar.common.CarType;
import com.quradar.driver.Driver;
import com.quradar.driver.DriverNotFoundException;
import com.quradar.driver.DriverRepository;
import com.quradar.fine.FineRepository;
import com.quradar.security.SecuritySupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

/** Registration is ADMIN-only; history allows OFFICER and own-vehicle CITIZEN. */
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleAdminController {

    private final VehicleRepository vehicles;
    private final DriverRepository drivers;
    private final FineRepository fines;
    private final SecuritySupport security;

    public VehicleAdminController(VehicleRepository vehicles, DriverRepository drivers,
                                  FineRepository fines, SecuritySupport security) {
        this.vehicles = vehicles;
        this.drivers = drivers;
        this.fines = fines;
        this.security = security;
    }

    public record VehicleCreateRequest(@NotBlank String plate, @NotNull CarType carType,
            String ownerLicenseNo) {
    }

    public record VehicleResponse(String plate, CarType carType, String ownerLicenseNo) {
        static VehicleResponse from(Vehicle vehicle) {
            String owner = vehicle.getOwner() != null ? vehicle.getOwner().getLicenseNo() : null;
            return new VehicleResponse(vehicle.getPlate(), vehicle.getCarType(), owner);
        }
    }

    public record VehicleHistoryResponse(VehicleResponse vehicle, String ownerName,
            Integer ownerPoints, List<FineEntry> fines) {
        public record FineEntry(Long id, int totalAmount, int violations) {
        }
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehicleResponse> register(@Valid @RequestBody VehicleCreateRequest request) {
        Driver owner = null;
        if (request.ownerLicenseNo() != null) {
            owner = drivers.findByLicenseNo(request.ownerLicenseNo())
                    .orElseThrow(() -> new DriverNotFoundException(request.ownerLicenseNo()));
        }
        Vehicle saved = vehicles.save(new Vehicle(request.plate(), request.carType(), owner));
        return ResponseEntity.status(HttpStatus.CREATED).body(VehicleResponse.from(saved));
    }

    @GetMapping("/{plate}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','CITIZEN')")
    public VehicleHistoryResponse history(@PathVariable String plate) {
        Vehicle vehicle = vehicles.findByPlate(plate)
                .orElseThrow(() -> new VehicleNotFoundException(plate));
        security.requireVehicleOwner(vehicle);
        List<VehicleHistoryResponse.FineEntry> history = fines
                .findByPlateNumberOrderByCreatedAtDesc(plate).stream()
                .map(f -> new VehicleHistoryResponse.FineEntry(f.getId(), f.getTotalAmount(),
                        f.getViolations().size()))
                .toList();
        Driver owner = vehicle.getOwner();
        return new VehicleHistoryResponse(VehicleResponse.from(vehicle),
                owner != null ? owner.getName() : null,
                owner != null ? owner.getPenaltyPoints() : null, history);
    }
}

package com.quradar.rules;

import com.quradar.common.RadarMetrics;
import com.quradar.device.DeviceEntity;
import com.quradar.device.DeviceNotFoundException;
import com.quradar.device.DeviceRepository;
import com.quradar.device.InactiveDeviceException;
import com.quradar.driver.Driver;
import com.quradar.driver.DriverRepository;
import com.quradar.fine.Fine;
import com.quradar.fine.FineCalculationService;
import com.quradar.fine.FineEntity;
import com.quradar.fine.FineRepository;
import com.quradar.ingestion.DuplicateEventException;
import com.quradar.ingestion.IdempotencyCache;
import com.quradar.ingestion.Observation;
import com.quradar.ingestion.ObservationEntity;
import com.quradar.ingestion.ObservationRepository;
import com.quradar.vehicle.VehicleRepository;
import com.quradar.violation.Violation;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Rule engine orchestrator. One transaction per observation: validate device,
 * run DB-configured rules, price via tiers, persist observation + fine +
 * violations, attribute penalty points to the plate owner. A duplicate eventId
 * violates the DB unique constraint and fails closed (409).
 *
 * Concurrency answer: Driver and FineEntity carry @Version. Two violations for
 * the same driver racing each other → one wins, the other gets
 * OptimisticLockingFailureException and retries (see P8 concurrency test).
 */
@Service
public class QuRadar {

    private final RuleConfigService ruleConfigs;
    private final FineCalculationService fineCalculation;
    private final IdempotencyCache idempotencyCache;
    private final RadarMetrics metrics;
    private final DeviceRepository devices;
    private final VehicleRepository vehicles;
    private final DriverRepository drivers;
    private final ObservationRepository observations;
    private final FineRepository fines;

    public QuRadar(RuleConfigService ruleConfigs, FineCalculationService fineCalculation,
                   IdempotencyCache idempotencyCache, RadarMetrics metrics,
                   DeviceRepository devices, VehicleRepository vehicles,
                   DriverRepository drivers, ObservationRepository observations,
                   FineRepository fines) {
        this.ruleConfigs = ruleConfigs;
        this.fineCalculation = fineCalculation;
        this.idempotencyCache = idempotencyCache;
        this.metrics = metrics;
        this.devices = devices;
        this.vehicles = vehicles;
        this.drivers = drivers;
        this.observations = observations;
        this.fines = fines;
    }

    @Transactional
    public Fine processObservation(Observation observation) {
        if (observation.getEventId() != null
                && idempotencyCache.seen(observation.getEventId(), observation.getDeviceCode())) {
            metrics.duplicate();
            throw new DuplicateEventException(observation.getEventId());
        }
        DeviceEntity device = null;
        if (observation.getDeviceCode() != null) {
            try {
                device = devices.findByDeviceCode(observation.getDeviceCode())
                        .orElseThrow(() -> new DeviceNotFoundException(observation.getDeviceCode()));
                if (!device.isActive()) {
                    throw new InactiveDeviceException(device.getDeviceCode());
                }
            } catch (DeviceNotFoundException | InactiveDeviceException ex) {
                metrics.badDevice();
                throw ex;
            }
            device.seen(null, currentIp());
        }
        metrics.received();

        List<Violation> violations = metrics.timed(() -> evaluate(observation));

        if (violations.isEmpty()) {
            return null;
        }

        ObservationEntity observationEntity = observations.save(new ObservationEntity(
                observation.getEventId(),
                device,
                observation.getPlateNumber(),
                observation.getDate(),
                observation.getCarType(),
                observation.getSpeed(),
                observation.isSeatbeltFastened(),
                observation.getLatitude(),
                observation.getLongitude(),
                observation.getLightState(),
                observation.isCrossedStopLine()));

        int total = violations.stream().mapToInt(Violation::getFee).sum();
        FineEntity fineEntity = new FineEntity(observation.getPlateNumber(), total, observationEntity);
        for (Violation violation : violations) {
            fineEntity.addViolation(violation.getRuleName(), violation.getDescription(),
                    violation.getFee(), violation.getPoints(), violation.getRuleVersion());
        }
        fines.save(fineEntity);
        metrics.created(violations.size());

        if (observation.getEventId() != null) {
            String eventId = observation.getEventId();
            String deviceCode = observation.getDeviceCode();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    idempotencyCache.mark(eventId, deviceCode);
                }
            });
        }

        vehicles.findByPlate(observation.getPlateNumber()).ifPresent(vehicle -> {
            Driver owner = vehicle.getOwner();
            if (owner != null) {
                Driver managed = drivers.findById(owner.getId()).orElseThrow();
                managed.addPenaltyPoints(violations.stream().mapToInt(Violation::getPoints).sum());
                drivers.save(managed);
            }
        });

        Fine fine = new Fine(observation.getPlateNumber(), violations);
        fine.print();

        return fine;
    }

    private List<Violation> evaluate(Observation observation) {
        List<ViolationRule> rules = ruleConfigs.buildEnabledRules();
        List<Violation> violations = new ArrayList<>();
        for (ViolationRule rule : rules) {
            if (rule.matches(observation)) {
                Violation violation = rule.evaluate(observation);
                if (violation != null) {
                    violation.setFee(fineCalculation.feeFor(rule, observation, violation.getFee()));
                    violation.setPoints(ruleConfigs.pointsFor(rule.getRuleCode()));
                    violation.setRuleVersion(ruleConfigs.versionFor(rule.getRuleCode()));
                    violations.add(violation);
                }
            }
        }
        return violations;
    }

    private static String currentIp() {
        try {
            var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest().getRemoteAddr() : null;
        } catch (Exception ex) {
            return null;
        }
    }
}

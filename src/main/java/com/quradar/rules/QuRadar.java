package com.quradar.rules;

import com.quradar.device.DeviceEntity;
import com.quradar.device.DeviceNotFoundException;
import com.quradar.device.DeviceRepository;
import com.quradar.device.InactiveDeviceException;
import com.quradar.fine.Fine;
import com.quradar.fine.FineEntity;
import com.quradar.fine.FineRepository;
import com.quradar.ingestion.Observation;
import com.quradar.ingestion.ObservationEntity;
import com.quradar.ingestion.ObservationRepository;
import com.quradar.violation.Violation;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rule engine orchestrator. One transaction per observation: validate device,
 * run DB-configured rules, persist observation + fine + violations atomically.
 * A duplicate eventId violates the DB unique constraint and fails closed (409).
 */
@Service
public class QuRadar {

    private final RuleConfigService ruleConfigs;
    private final DeviceRepository devices;
    private final ObservationRepository observations;
    private final FineRepository fines;

    public QuRadar(RuleConfigService ruleConfigs, DeviceRepository devices,
                   ObservationRepository observations, FineRepository fines) {
        this.ruleConfigs = ruleConfigs;
        this.devices = devices;
        this.observations = observations;
        this.fines = fines;
    }

    @Transactional
    public Fine processObservation(Observation observation) {
        DeviceEntity device = null;
        if (observation.getDeviceCode() != null) {
            device = devices.findByDeviceCode(observation.getDeviceCode())
                    .orElseThrow(() -> new DeviceNotFoundException(observation.getDeviceCode()));
            if (!device.isActive()) {
                throw new InactiveDeviceException(device.getDeviceCode());
            }
        }

        List<Violation> violations = new ArrayList<>();
        for (ViolationRule rule : ruleConfigs.buildEnabledRules()) {
            if (rule.matches(observation)) {
                Violation violation = rule.evaluate(observation);
                if (violation != null) {
                    violations.add(violation);
                }
            }
        }

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
            fineEntity.addViolation(violation.getRuleName(), violation.getDescription(), violation.getFee());
        }
        fines.save(fineEntity);

        Fine fine = new Fine(observation.getPlateNumber(), violations);
        fine.print();

        return fine;
    }
}

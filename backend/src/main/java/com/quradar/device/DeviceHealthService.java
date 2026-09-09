package com.quradar.device;

import com.quradar.ingestion.ObservationRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Health is DERIVED from last-seen age, never stored: ACTIVE below the degraded
 * threshold, DEGRADED below the offline threshold, OFFLINE beyond it (or never
 * seen). Thresholds are configurable; defaults are 2 and 5 minutes.
 */
@Service
public class DeviceHealthService {

    private final ObservationRepository observations;
    private final long degradedMinutes;
    private final long offlineMinutes;

    public DeviceHealthService(ObservationRepository observations,
                               @Value("${quradar.device.degraded-minutes:2}") long degradedMinutes,
                               @Value("${quradar.device.offline-minutes:5}") long offlineMinutes) {
        this.observations = observations;
        this.degradedMinutes = degradedMinutes;
        this.offlineMinutes = offlineMinutes;
    }

    public DeviceHealth healthOf(DeviceEntity device) {
        if (device.getLastSeenAt() == null) {
            return DeviceHealth.OFFLINE;
        }
        long ageMinutes = Duration.between(device.getLastSeenAt(), Instant.now()).toMinutes();
        if (ageMinutes >= offlineMinutes) {
            return DeviceHealth.OFFLINE;
        }
        if (ageMinutes >= degradedMinutes) {
            return DeviceHealth.DEGRADED;
        }
        return DeviceHealth.ACTIVE;
    }

    public record DeviceDetail(String deviceCode, String name, boolean active,
            DeviceHealth health, Instant lastSeenAt, String firmwareVersion, String lastIp,
            Instant registeredAt, long eventCount, Instant lastEventAt) {
    }

    @Transactional(readOnly = true)
    public DeviceDetail detail(DeviceEntity device) {
        long events = observations.countByDeviceId(device.getId());
        Instant lastEvent = observations.findTopByDeviceIdOrderByCreatedAtDesc(device.getId())
                .map(o -> o.getCreatedAt()).orElse(null);
        return new DeviceDetail(device.getDeviceCode(), device.getName(), device.isActive(),
                healthOf(device), device.getLastSeenAt(), device.getFirmwareVersion(),
                device.getLastIp(), device.getCreatedAt(), events, lastEvent);
    }
}

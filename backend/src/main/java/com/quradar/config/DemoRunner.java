package com.quradar.config;

import com.quradar.common.CarType;
import com.quradar.fine.FineQueryService;
import com.quradar.ingestion.LightState;
import com.quradar.ingestion.Observation;
import com.quradar.rules.QuRadar;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Demo runner carrying the original scenario; replaced by REST ingestion.
 * Disabled in tests via quradar.demo.enabled=false. Uses fresh eventIds per
 * boot so re-runs never trip the idempotency constraint.
 */
@Component
@ConditionalOnProperty(prefix = "quradar.demo", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class DemoRunner implements ApplicationRunner {

    private final QuRadar radar;
    private final FineQueryService queries;

    public DemoRunner(QuRadar radar, FineQueryService queries) {
        this.radar = radar;
        this.queries = queries;
    }

    private static Observation observation(String plate, CarType type, int speed, boolean belt) {
        return new Observation(UUID.randomUUID().toString(), "RADAR-001", plate, LocalDate.now(),
                type, speed, belt, null, null, LightState.GREEN, false);
    }

    @Override
    public void run(ApplicationArguments args) {
        radar.processObservation(observation("ABC1234", CarType.PRIVATE, 94, false));
        radar.processObservation(observation("XYZ777", CarType.TRUCK, 75, true));
        radar.processObservation(observation("CLN001", CarType.PRIVATE, 60, true));
        radar.processObservation(observation("ABC1234", CarType.PRIVATE, 90, true));

        System.out.println();
        System.out.println("=== All possible fines ===");

        for (Map.Entry<String, Integer> entry : queries.getTotalFinesByPlate().entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue() + " EGP");
        }

        System.out.println();
        System.out.println("=== All violated rules ===");

        for (Map.Entry<String, Long> entry : queries.getViolationCountsByRule().entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }
}

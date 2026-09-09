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
 * Seeded demo traffic spread over the last week so the trend chart shows a
 * real distribution instead of a single-day spike. Uses fresh eventIds per
 * boot so re-runs never trip the idempotency constraint. Replaced by REST
 * ingestion; disabled in tests via quradar.demo.enabled=false.
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

    private static Observation observation(String plate, CarType type, int speed, boolean belt,
                                           Double lat, Double lon, LightState light,
                                           boolean crossed, int daysAgo) {
        return new Observation(UUID.randomUUID().toString(), "RADAR-001", plate,
                LocalDate.now().minusDays(daysAgo), type, speed, belt, lat, lon, light, crossed);
    }

    @Override
    public void run(ApplicationArguments args) {
        radar.processObservation(observation("ABC1234", CarType.PRIVATE, 94, false,
                null, null, LightState.GREEN, false, 6));
        radar.processObservation(observation("XYZ777", CarType.TRUCK, 75, true,
                null, null, LightState.GREEN, false, 6));
        radar.processObservation(observation("CLN001", CarType.PRIVATE, 60, true,
                null, null, LightState.GREEN, false, 5));
        radar.processObservation(observation("ABC1234", CarType.PRIVATE, 90, true,
                null, null, LightState.GREEN, false, 4));
        radar.processObservation(observation("XYZ777", CarType.TRUCK, 55, true,
                null, null, LightState.GREEN, false, 3));
        radar.processObservation(observation("ABC1234", CarType.PRIVATE, 70, true,
                30.05, 31.20, LightState.GREEN, false, 2));
        radar.processObservation(observation("NEWBIE1", CarType.PRIVATE, 100, true,
                null, null, LightState.GREEN, false, 2));
        radar.processObservation(observation("ABC1234", CarType.PRIVATE, 65, true,
                null, null, LightState.RED, true, 1));
        radar.processObservation(observation("XYZ777", CarType.TRUCK, 90, false,
                null, null, LightState.GREEN, false, 1));
        radar.processObservation(observation("ABC1234", CarType.PRIVATE, 85, true,
                null, null, LightState.GREEN, false, 0));

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

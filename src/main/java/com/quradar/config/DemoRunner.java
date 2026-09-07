package com.quradar.config;

import com.quradar.common.CarType;
import com.quradar.fine.FineQueryService;
import com.quradar.ingestion.LightState;
import com.quradar.ingestion.Observation;
import com.quradar.rules.QuRadar;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Demo runner carrying the original scenario; replaced by REST ingestion in P4. */
@Component
public class DemoRunner implements ApplicationRunner {

    private final QuRadar radar;
    private final FineQueryService queries;

    public DemoRunner(QuRadar radar, FineQueryService queries) {
        this.radar = radar;
        this.queries = queries;
    }

    @Override
    public void run(ApplicationArguments args) {
        radar.processObservation(new Observation("ABC1234", LocalDate.now(), CarType.PRIVATE, 94,
                false, null, null, LightState.GREEN, false));
        radar.processObservation(new Observation("XYZ777", LocalDate.now(), CarType.TRUCK, 75,
                true, null, null, LightState.GREEN, false));
        radar.processObservation(new Observation("CLN001", LocalDate.now(), CarType.PRIVATE, 60,
                true, null, null, LightState.GREEN, false));
        radar.processObservation(new Observation("ABC1234", LocalDate.now(), CarType.PRIVATE, 90,
                true, null, null, LightState.GREEN, false));

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

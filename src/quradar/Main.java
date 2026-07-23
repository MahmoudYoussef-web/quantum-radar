package quradar;

import java.time.LocalDate;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        QuRadar radar = new QuRadar();

        radar.addRule(new SeatbeltRule());
        radar.addRule(new SpeedLimitRule(CarType.TRUCK, 60, 300));
        radar.addRule(new SpeedLimitRule(CarType.PRIVATE, 80, 300));

        radar.processObservation(new Observation("ABC1234", LocalDate.now(), CarType.PRIVATE, 94, false));
        radar.processObservation(new Observation("XYZ777", LocalDate.now(), CarType.TRUCK, 75, true));
        radar.processObservation(new Observation("CLN001", LocalDate.now(), CarType.PRIVATE, 60, true));
        radar.processObservation(new Observation("ABC1234", LocalDate.now(), CarType.PRIVATE, 90, true));

        System.out.println();
        System.out.println("=== All possible fines ===");

        Map<String, Integer> fines = radar.getAllPossibleFines();
        for (Map.Entry<String, Integer> entry : fines.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue() + " EGP");
        }

        System.out.println();
        System.out.println("=== All violated rules ===");

        Map<String, Integer> violatedRules = radar.getAllViolatedRules();
        for (Map.Entry<String, Integer> entry : violatedRules.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }
}

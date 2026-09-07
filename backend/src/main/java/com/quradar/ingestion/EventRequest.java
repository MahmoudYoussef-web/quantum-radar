package com.quradar.ingestion;

import com.quradar.common.CarType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EventRequest(
        @NotBlank String eventId,
        @NotBlank String deviceCode,
        @NotBlank String plateNumber,
        @NotNull LocalDate observedAt,
        @NotNull CarType carType,
        @Min(0) int speed,
        boolean seatbeltFastened,
        Double latitude,
        Double longitude,
        LightState lightState,
        boolean crossedStopLine) {

    public Observation toObservation() {
        return new Observation(eventId, deviceCode, plateNumber, observedAt, carType, speed,
                seatbeltFastened, latitude, longitude, lightState, crossedStopLine);
    }
}

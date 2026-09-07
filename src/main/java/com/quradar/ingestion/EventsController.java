package com.quradar.ingestion;

import com.quradar.fine.Fine;
import com.quradar.fine.FineResponse;
import com.quradar.rules.QuRadar;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * NOTE: no JWT until P6 — local-only. P6 restricts this to DEVICE (own device only).
 * A duplicate eventId fails closed with 409 via the DB unique constraint.
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventsController {

    private final QuRadar radar;

    public EventsController(QuRadar radar) {
        this.radar = radar;
    }

    @PostMapping
    public ResponseEntity<FineResponse> ingest(@Valid @RequestBody EventRequest request) {
        Fine fine = radar.processObservation(request.toObservation());
        if (fine == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(FineResponse.from(fine));
    }
}

package com.quradar.ingestion;

import com.quradar.fine.Fine;
import com.quradar.fine.FineResponse;
import com.quradar.rules.QuRadar;
import com.quradar.security.SecuritySupport;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEVICE may submit only for its own device (enforced server-side against the
 * JWT identity, never the request body alone); ADMIN may submit for any.
 * A duplicate eventId fails closed with 409 via the DB unique constraint.
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventsController {

    private final QuRadar radar;
    private final SecuritySupport security;

    public EventsController(QuRadar radar, SecuritySupport security) {
        this.radar = radar;
        this.security = security;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DEVICE','ADMIN')")
    public ResponseEntity<FineResponse> ingest(@Valid @RequestBody EventRequest request) {
        security.requireDeviceOwner(request.deviceCode());
        Fine fine = radar.processObservation(request.toObservation());
        if (fine == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(FineResponse.from(fine));
    }
}

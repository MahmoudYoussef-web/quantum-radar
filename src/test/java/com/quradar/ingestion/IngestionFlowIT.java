package com.quradar.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full ingestion flow against real Postgres: first submission persists (201),
 * resubmission of the same eventId fails closed (409) and creates nothing new.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quradar.demo.enabled=false")
@Testcontainers
class IngestionFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObservationRepository observations;

    private Map<String, Object> event(String eventId) {
        return Map.of(
                "eventId", eventId,
                "deviceCode", "RADAR-001",
                "plateNumber", "TST-" + eventId.substring(0, 4),
                "observedAt", LocalDate.now().toString(),
                "carType", "PRIVATE",
                "speed", 95,
                "seatbeltFastened", true);
    }

    @Test
    void duplicateEventIdIsRejectedWithoutDoubleFine() {
        String eventId = UUID.randomUUID().toString();
        long before = observations.count();

        ResponseEntity<Map> first = rest.postForEntity("/api/v1/events", event(eventId), Map.class);
        assertEquals(HttpStatus.CREATED, first.getStatusCode());
        assertEquals(before + 1, observations.count());

        ResponseEntity<Map> replay = rest.postForEntity("/api/v1/events", event(eventId), Map.class);
        assertEquals(HttpStatus.CONFLICT, replay.getStatusCode());
        assertEquals(before + 1, observations.count());
    }

    @Test
    void unknownDeviceIsRejected() {
        Map<String, Object> body = event(UUID.randomUUID().toString());
        body = new java.util.HashMap<>(body);
        body.put("deviceCode", "GHOST-9");
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/events", body, Map.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void invalidPayloadIsRejected() {
        ResponseEntity<Map> response =
                rest.postForEntity("/api/v1/events", Map.of("eventId", "x"), Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}

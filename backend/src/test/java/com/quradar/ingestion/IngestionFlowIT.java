package com.quradar.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full ingestion flow against real Postgres with JWT auth: first submission
 * persists (201), resubmission of the same eventId fails closed (409) and
 * creates nothing new; unauthenticated calls are rejected (401).
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

    private String adminToken;

    @BeforeEach
    void login() {
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login",
                Map.of("username", "admin", "password", "admin123"), Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        adminToken = "Bearer " + response.getBody().get("accessToken");
    }

    private HttpEntity<Map<String, Object>> authorized(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", adminToken);
        return new HttpEntity<>(body, headers);
    }

    private Map<String, Object> event(String eventId) {
        return new java.util.HashMap<>(Map.of(
                "eventId", eventId,
                "deviceCode", "RADAR-001",
                "plateNumber", "TST-" + eventId.substring(0, 4),
                "observedAt", LocalDate.now().toString(),
                "carType", "PRIVATE",
                "speed", 95,
                "seatbeltFastened", true));
    }

    private HttpEntity<Void> authorizedGet() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", adminToken);
        return new HttpEntity<>(null, headers);
    }

    @Test
    void statsReflectIngestedViolations() {
        String eventId = UUID.randomUUID().toString();
        ResponseEntity<Map> first = rest.exchange("/api/v1/events", HttpMethod.POST,
                authorized(event(eventId)), Map.class);
        assertEquals(HttpStatus.CREATED, first.getStatusCode());

        ResponseEntity<java.util.List> daily = rest.exchange(
                "/api/v1/violations/stats/daily?days=7", HttpMethod.GET, authorizedGet(),
                java.util.List.class);
        assertEquals(HttpStatus.OK, daily.getStatusCode());
        org.junit.jupiter.api.Assertions.assertFalse(daily.getBody().isEmpty());

        ResponseEntity<java.util.List> byRule = rest.exchange("/api/v1/violations/stats/by-rule",
                HttpMethod.GET, authorizedGet(), java.util.List.class);
        assertEquals(HttpStatus.OK, byRule.getStatusCode());
        org.junit.jupiter.api.Assertions.assertFalse(byRule.getBody().isEmpty());
    }

    @Test
    void duplicateEventIdIsRejectedWithoutDoubleFine() {
        String eventId = UUID.randomUUID().toString();
        long before = observations.count();

        ResponseEntity<Map> first = rest.exchange("/api/v1/events", HttpMethod.POST,
                authorized(event(eventId)), Map.class);
        assertEquals(HttpStatus.CREATED, first.getStatusCode());
        assertEquals(before + 1, observations.count());

        ResponseEntity<Map> replay = rest.exchange("/api/v1/events", HttpMethod.POST,
                authorized(event(eventId)), Map.class);
        assertEquals(HttpStatus.CONFLICT, replay.getStatusCode());
        assertEquals(before + 1, observations.count());
    }

    @Test
    void sameEventIdFromAnotherDeviceIsAccepted() {
        ResponseEntity<Map> device = rest.exchange("/api/v1/devices", HttpMethod.POST,
                authorized(new java.util.HashMap<>(
                        Map.of("deviceCode", "RADAR-002", "name", "Second radar"))),
                Map.class);
        assertEquals(HttpStatus.CREATED, device.getStatusCode());

        String eventId = UUID.randomUUID().toString();
        Map<String, Object> first = event(eventId);
        ResponseEntity<Map> one = rest.exchange("/api/v1/events", HttpMethod.POST,
                authorized(first), Map.class);
        assertEquals(HttpStatus.CREATED, one.getStatusCode());

        Map<String, Object> second = event(eventId);
        second.put("deviceCode", "RADAR-002");
        ResponseEntity<Map> two = rest.exchange("/api/v1/events", HttpMethod.POST,
                authorized(second), Map.class);
        assertEquals(HttpStatus.CREATED, two.getStatusCode());
    }

    @Test
    void unknownDeviceIsRejected() {
        Map<String, Object> body = event(UUID.randomUUID().toString());
        body.put("deviceCode", "GHOST-9");
        ResponseEntity<Map> response = rest.exchange("/api/v1/events", HttpMethod.POST,
                authorized(body), Map.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void invalidPayloadIsRejected() {
        ResponseEntity<Map> response = rest.exchange("/api/v1/events", HttpMethod.POST,
                authorized(Map.of("eventId", "x")), Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void unauthenticatedIsRejected() {
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/events",
                event(UUID.randomUUID().toString()), Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void wrongPasswordIsRejected() {
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login",
                Map.of("username", "admin", "password", "wrong"), Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}

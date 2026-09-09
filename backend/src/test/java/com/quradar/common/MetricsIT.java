package com.quradar.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
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

/** Health is public; metrics need ADMIN; domain counters move with traffic. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quradar.demo.enabled=false")
@Testcontainers
class MetricsIT {

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

    @Test
    void healthIsPublicAndMetricsNeedAuth() {
        ResponseEntity<Map> health =
                rest.getForEntity("/actuator/health", Map.class);
        assertEquals(HttpStatus.OK, health.getStatusCode());
        assertEquals("UP", health.getBody().get("status"));

        ResponseEntity<String> denied =
                rest.getForEntity("/actuator/metrics/quradar.events.received", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, denied.getStatusCode());
    }

    @Test
    void countersMoveWithIngestion() {
        ResponseEntity<Map> login = rest.postForEntity("/api/v1/auth/login",
                Map.of("username", "admin", "password", "admin123"), Map.class);
        String token = "Bearer " + login.getBody().get("accessToken");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);

        Map<String, Object> event = new java.util.HashMap<>(Map.of(
                "eventId", UUID.randomUUID().toString(),
                "deviceCode", "RADAR-001",
                "plateNumber", "MET-1",
                "observedAt", LocalDate.now().toString(),
                "carType", "PRIVATE",
                "speed", 95,
                "seatbeltFastened", true));
        ResponseEntity<Map> ingested = rest.exchange("/api/v1/events", HttpMethod.POST,
                new HttpEntity<>(event, headers), Map.class);
        assertEquals(HttpStatus.CREATED, ingested.getStatusCode());

        ResponseEntity<Map> metrics = rest.exchange(
                "/actuator/metrics/quradar.events.received", HttpMethod.GET,
                new HttpEntity<>(null, headers), Map.class);
        assertEquals(HttpStatus.OK, metrics.getStatusCode());
        java.util.List<Map<String, Object>> measurements =
                (java.util.List<Map<String, Object>>) metrics.getBody().get("measurements");
        double received = ((Number) measurements.get(0).get("value")).doubleValue();
        assertTrue(received >= 1.0);

        ResponseEntity<Map> duplicate = rest.exchange("/api/v1/events", HttpMethod.POST,
                new HttpEntity<>(event, headers), Map.class);
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());

        ResponseEntity<Map> rejected = rest.exchange(
                "/actuator/metrics/quradar.events.rejected", HttpMethod.GET,
                new HttpEntity<>(null, headers), Map.class);
        assertEquals(HttpStatus.OK, rejected.getStatusCode());
        java.util.List<Map<String, Object>> availableTags =
                (java.util.List<Map<String, Object>>) rejected.getBody().get("availableTags");
        assertTrue(availableTags.stream()
                .anyMatch(t -> "reason".equals(t.get("tag"))
                        && ((java.util.List<String>) t.get("values")).contains("duplicate")));
    }
}

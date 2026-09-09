package com.quradar.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
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

/**
 * Rule tuning snapshots versions; new violations pin the version they were
 * judged under, so old fines never move when a rule changes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quradar.demo.enabled=false")
@Testcontainers
class RuleVersionIT {

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

    private String adminToken() {
        ResponseEntity<Map> login = rest.postForEntity("/api/v1/auth/login",
                Map.of("username", "admin", "password", "admin123"), Map.class);
        assertEquals(HttpStatus.OK, login.getStatusCode());
        return "Bearer " + login.getBody().get("accessToken");
    }

    private HttpEntity<Map<String, Object>> authorized(Map<String, Object> body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void tuningSnapshotsVersionsAndPinsViolations() {
        String token = adminToken();

        rest.exchange("/api/v1/rules/SEATBELT", HttpMethod.PATCH,
                authorized(Map.of("fee", 150), token), Map.class);
        rest.exchange("/api/v1/rules/SEATBELT", HttpMethod.PATCH,
                authorized(Map.of("fee", 200), token), Map.class);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        ResponseEntity<List> versions = rest.exchange("/api/v1/rules/SEATBELT/versions",
                HttpMethod.GET, new HttpEntity<>(null, headers), List.class);
        assertEquals(HttpStatus.OK, versions.getStatusCode());
        assertEquals(3, versions.getBody().size());

        Map<String, Object> event = new java.util.HashMap<>(Map.of(
                "eventId", UUID.randomUUID().toString(),
                "deviceCode", "RADAR-001",
                "plateNumber", "VER-1",
                "observedAt", LocalDate.now().toString(),
                "carType", "PRIVATE",
                "speed", 50,
                "seatbeltFastened", false));
        ResponseEntity<Map> fine = rest.exchange("/api/v1/events", HttpMethod.POST,
                authorized(event, token), Map.class);
        assertEquals(HttpStatus.CREATED, fine.getStatusCode());

        java.util.List<Map<String, Object>> violations =
                (java.util.List<Map<String, Object>>) fine.getBody().get("violations");
        assertTrue(violations.stream().anyMatch(v -> "SEATBELT".equals(v.get("ruleName"))
                && Integer.valueOf(3).equals(v.get("ruleVersion"))
                && Integer.valueOf(200).equals(v.get("fee"))));
    }
}

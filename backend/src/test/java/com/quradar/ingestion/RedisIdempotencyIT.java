package com.quradar.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quradar.fine.FineRepository;
import com.quradar.violation.ViolationRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Redis fast-path: replay is rejected from cache even after the DB row is gone. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quradar.demo.enabled=false")
@Testcontainers
class RedisIdempotencyIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObservationRepository observations;

    @Autowired
    private FineRepository fines;

    @Autowired
    private ViolationRepository violations;

    private Map<String, Object> event(String eventId) {
        return new java.util.HashMap<>(Map.of(
                "eventId", eventId,
                "deviceCode", "RADAR-001",
                "plateNumber", "RDS-1",
                "observedAt", LocalDate.now().toString(),
                "carType", "PRIVATE",
                "speed", 95,
                "seatbeltFastened", true));
    }

    private String adminToken() {
        ResponseEntity<Map> login = rest.postForEntity("/api/v1/auth/login",
                Map.of("username", "admin", "password", "admin123"), Map.class);
        return "Bearer " + login.getBody().get("accessToken");
    }

    @Test
    void replayRejectedFromCacheAfterDbRowDeleted() {
        String token = adminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);

        String eventId = UUID.randomUUID().toString();
        ResponseEntity<Map> first = rest.exchange("/api/v1/events", HttpMethod.POST,
                new HttpEntity<>(event(eventId), headers), Map.class);
        assertEquals(HttpStatus.CREATED, first.getStatusCode());
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey("idem:event:RADAR-001:" + eventId)));

        violations.deleteAll();
        fines.deleteAll();
        observations.deleteAll();

        ResponseEntity<Map> replay = rest.exchange("/api/v1/events", HttpMethod.POST,
                new HttpEntity<>(event(eventId), headers), Map.class);
        assertEquals(HttpStatus.CONFLICT, replay.getStatusCode());
    }
}

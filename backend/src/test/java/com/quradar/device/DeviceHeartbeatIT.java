package com.quradar.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
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

/** Heartbeats mark devices seen; health derives from last-seen age. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quradar.demo.enabled=false")
@Testcontainers
class DeviceHeartbeatIT {

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

    @Test
    void heartbeatMarksDeviceSeenWithHealth() {
        String token = adminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);

        ResponseEntity<Map> heartbeat = rest.exchange("/api/v1/devices/RADAR-001/heartbeat",
                HttpMethod.POST, new HttpEntity<>(Map.of("firmwareVersion", "1.4.2"), headers),
                Map.class);
        assertEquals(HttpStatus.OK, heartbeat.getStatusCode());
        assertEquals("ACTIVE", heartbeat.getBody().get("health"));
        assertNotNull(heartbeat.getBody().get("lastSeenAt"));

        ResponseEntity<Map> detail = rest.exchange("/api/v1/devices/RADAR-001", HttpMethod.GET,
                new HttpEntity<>(null, headers), Map.class);
        assertEquals(HttpStatus.OK, detail.getStatusCode());
        assertEquals("ACTIVE", detail.getBody().get("health"));
        assertEquals("1.4.2", detail.getBody().get("firmwareVersion"));
    }

    @Test
    void unknownDeviceHeartbeatIs404() {
        String token = adminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);

        ResponseEntity<Map> heartbeat = rest.exchange("/api/v1/devices/GHOST-9/heartbeat",
                HttpMethod.POST, new HttpEntity<>(Map.of(), headers), Map.class);
        assertEquals(HttpStatus.NOT_FOUND, heartbeat.getStatusCode());
    }
}

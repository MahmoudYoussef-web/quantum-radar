package com.quradar.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

/** Admin mutations leave a who-did-what trail readable from the audit API. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quradar.demo.enabled=false")
@Testcontainers
class AuditIT {

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
    void ruleUpdateIsAudited() {
        String token = adminToken();

        ResponseEntity<Map> update = rest.exchange("/api/v1/rules/SEATBELT", HttpMethod.PATCH,
                authorized(Map.of("fee", 150), token), Map.class);
        assertEquals(HttpStatus.OK, update.getStatusCode());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        ResponseEntity<Map> audit = rest.exchange("/api/v1/audit?page=0&size=5", HttpMethod.GET,
                new HttpEntity<>(null, headers), Map.class);
        assertEquals(HttpStatus.OK, audit.getStatusCode());

        java.util.List<Map<String, Object>> content =
                (java.util.List<Map<String, Object>>) audit.getBody().get("content");
        assertTrue(content.stream().anyMatch(e -> "UPDATED_RULE".equals(e.get("action"))
                && "admin".equals(e.get("actor"))
                && "SEATBELT".equals(e.get("entityId"))));
    }
}

package com.quradar.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
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

/** Auth lifecycle over real HTTP+PG: login, rotation, reuse-rejection, logout. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quradar.demo.enabled=false")
@Testcontainers
class AuthFlowIT {

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

    private Map<String, String> login(String username, String password) {
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login",
                Map.of("username", username, "password", password), Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return (Map<String, String>) response.getBody();
    }

    @Test
    void loginRefreshRotationAndLogout() {
        Map<String, String> pair = login("admin", "admin123");

        Map<String, String> rotated = rest.postForEntity("/api/v1/auth/refresh",
                Map.of("refreshToken", pair.get("refreshToken")), Map.class).getBody();
        assertNotEquals(pair.get("refreshToken"), rotated.get("refreshToken"));

        ResponseEntity<Map> reuse = rest.postForEntity("/api/v1/auth/refresh",
                Map.of("refreshToken", pair.get("refreshToken")), Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, reuse.getStatusCode());

        ResponseEntity<Void> logout = rest.postForEntity("/api/v1/auth/logout",
                Map.of("refreshToken", rotated.get("refreshToken")), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, logout.getStatusCode());

        ResponseEntity<Map> afterLogout = rest.postForEntity("/api/v1/auth/refresh",
                Map.of("refreshToken", rotated.get("refreshToken")), Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, afterLogout.getStatusCode());
    }

    @Test
    void badCredentialsRejected() {
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login",
                Map.of("username", "admin", "password", "nope"), Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}

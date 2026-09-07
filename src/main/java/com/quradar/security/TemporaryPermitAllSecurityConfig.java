package com.quradar.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TEMPORARY (P4-P5 only): permit-all so the ingestion/device/rules APIs are usable locally.
 * Standing constraint: never expose publicly before P6. DELETED in P6 and replaced by
 * JWT auth with roles (ADMIN/OFFICER/DEVICE/CITIZEN) + server-side ownership enforcement.
 */
@Configuration
@EnableWebSecurity
public class TemporaryPermitAllSecurityConfig {

    @Bean
    SecurityFilterChain permitAll(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}

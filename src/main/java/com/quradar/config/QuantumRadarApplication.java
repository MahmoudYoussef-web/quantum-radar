package com.quradar.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.quradar")
@EnableJpaRepositories(basePackages = "com.quradar")
@EntityScan(basePackages = "com.quradar")
public class QuantumRadarApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuantumRadarApplication.class, args);
    }
}

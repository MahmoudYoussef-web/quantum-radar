package com.quradar.driver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Two concurrent penalty-point updates for the same driver: both read version N,
 * both write, exactly one commit wins and the loser gets
 * OptimisticLockingFailureException (and would retry in production code).
 */
@SpringBootTest(properties = "quradar.demo.enabled=false")
@Testcontainers
class OptimisticLockingIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DriverRepository drivers;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void concurrentPointUpdatesConflict() throws Exception {
        Driver driver = drivers.save(new Driver("Racer", "RACE-001"));
        Long driverId = driver.getId();

        CountDownLatch bothRead = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger conflicts = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> addPoints(driverId, 3, bothRead, release, conflicts));
            Future<?> second = pool.submit(() -> addPoints(driverId, 5, bothRead, release, conflicts));
            bothRead.await();
            release.countDown();
            first.get();
            second.get();
        } finally {
            pool.shutdown();
        }

        assertEquals(1, conflicts.get());
        Driver reloaded = drivers.findById(driverId).orElseThrow();
        assertTrue(reloaded.getPenaltyPoints() == 3 || reloaded.getPenaltyPoints() == 5);
    }

    private void addPoints(Long driverId, int points, CountDownLatch bothRead,
                           CountDownLatch release, AtomicInteger conflicts) {
        try {
            new TransactionTemplate(transactionManager).execute(status -> {
                Driver managed = drivers.findById(driverId).orElseThrow();
                bothRead.countDown();
                try {
                    release.await();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                managed.addPenaltyPoints(points);
                drivers.saveAndFlush(managed);
                return null;
            });
        } catch (ObjectOptimisticLockingFailureException ex) {
            conflicts.incrementAndGet();
        }
    }
}

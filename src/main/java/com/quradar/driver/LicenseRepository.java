package com.quradar.driver;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseRepository extends JpaRepository<License, Long> {

    Optional<License> findByDriverId(Long driverId);
}

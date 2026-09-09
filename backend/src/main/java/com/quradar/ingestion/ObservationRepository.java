package com.quradar.ingestion;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservationRepository extends JpaRepository<ObservationEntity, Long> {

    long countByDeviceId(Long deviceId);

    Optional<ObservationEntity> findTopByDeviceIdOrderByCreatedAtDesc(Long deviceId);
}

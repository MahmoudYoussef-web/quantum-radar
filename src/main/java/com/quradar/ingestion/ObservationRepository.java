package com.quradar.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservationRepository extends JpaRepository<ObservationEntity, Long> {
}

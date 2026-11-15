package com.runtimeheal.backend.persistence.repository;

import com.runtimeheal.backend.persistence.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, String> {
    List<IncidentEntity> findAllByOrderByTimestampDesc(Pageable pageable);
}

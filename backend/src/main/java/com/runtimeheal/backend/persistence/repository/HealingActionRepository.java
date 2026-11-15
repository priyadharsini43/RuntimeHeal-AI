package com.runtimeheal.backend.persistence.repository;

import com.runtimeheal.backend.persistence.entity.HealingActionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealingActionRepository extends JpaRepository<HealingActionEntity, String> {
    List<HealingActionEntity> findAllByOrderByTimestampDesc(Pageable pageable);
}

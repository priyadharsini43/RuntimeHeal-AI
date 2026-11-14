package com.runtimeheal.backend.persistence.repository;

import com.runtimeheal.backend.persistence.entity.DiagnosticReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiagnosticReportRepository extends JpaRepository<DiagnosticReportEntity, String> {
    List<DiagnosticReportEntity> findAllByOrderByTimestampDesc();
}

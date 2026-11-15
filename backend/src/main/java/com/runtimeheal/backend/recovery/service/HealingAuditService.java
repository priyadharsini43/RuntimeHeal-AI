package com.runtimeheal.backend.recovery.service;

import com.runtimeheal.backend.persistence.entity.HealingActionEntity;
import com.runtimeheal.backend.persistence.repository.HealingActionRepository;
import org.springframework.data.domain.PageRequest;
import com.runtimeheal.backend.recovery.model.HealingActionRecord;
import com.runtimeheal.backend.recovery.model.HealingStatus;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Audit repository storing and aggregating self-healing execution actions and telemetry metrics.
 */
@Service
public class HealingAuditService {

    private static final Logger log = LoggerFactory.getLogger(HealingAuditService.class);
    private static final int MAX_AUDIT_RECORDS = 1000;

    private final HealingActionRepository actionRepository;
    private final List<HealingActionRecord> auditLog = new CopyOnWriteArrayList<>();
    private final Map<String, HealingActionRecord> actionIndex = new ConcurrentHashMap<>();

    public HealingAuditService() {
        this(null);
    }

    @Autowired
    public HealingAuditService(@Autowired(required = false) HealingActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    @PostConstruct
    public void init() {
        if (actionRepository != null) {
            try {
                List<HealingActionEntity> saved = actionRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, MAX_AUDIT_RECORDS));
                List<HealingActionEntity> chronological = new ArrayList<>(saved);
                Collections.reverse(chronological);
                for (HealingActionEntity entity : chronological) {
                    HealingActionRecord record = entity.toModel();
                    auditLog.add(record);
                    actionIndex.put(record.getActionId(), record);
                }
                log.info("[Healing Audit] Hydrated {} persisted healing records from database", saved.size());
            } catch (Exception e) {
                log.warn("[Healing Audit] Could not hydrate healing actions from database: {}", e.getMessage());
            }
        }
    }

    public synchronized HealingActionRecord recordAction(HealingActionRecord record) {
        if (record == null || record.getActionId() == null) {
            return null;
        }

        if (auditLog.size() >= MAX_AUDIT_RECORDS) {
            HealingActionRecord oldest = auditLog.remove(0);
            if (oldest != null) {
                actionIndex.remove(oldest.getActionId());
            }
        }

        auditLog.add(record);
        actionIndex.put(record.getActionId(), record);

        if (actionRepository != null) {
            try {
                actionRepository.save(HealingActionEntity.fromModel(record));
            } catch (Exception e) {
                log.warn("[Healing Audit] Failed to persist action {}: {}", record.getActionId(), e.getMessage());
            }
        }

        log.info("[Healing Audit] Action {} | Method: {}.{}() | Strategy: {} | Status: {} | Reason: {}",
                record.getActionId(), record.getClassName(), record.getMethodName(),
                record.getStrategy(), record.getStatus(), record.getPolicyReason());

        return record;
    }

    public List<HealingActionRecord> getAllActions() {
        List<HealingActionRecord> reversed = new ArrayList<>(auditLog);
        Collections.reverse(reversed);
        return Collections.unmodifiableList(reversed);
    }

    public Optional<HealingActionRecord> getActionById(String actionId) {
        return Optional.ofNullable(actionIndex.get(actionId));
    }

    public Map<String, Object> getAuditSummary() {
        long total = auditLog.size();
        long success = auditLog.stream().filter(r -> r.getStatus() == HealingStatus.SUCCESS).count();
        long blockedPolicy = auditLog.stream().filter(r -> r.getStatus() == HealingStatus.BLOCKED_POLICY).count();
        long blockedCb = auditLog.stream().filter(r -> r.getStatus() == HealingStatus.BLOCKED_CIRCUIT_BREAKER).count();
        long lowConfidence = auditLog.stream().filter(r -> r.getStatus() == HealingStatus.SKIPPED_LOW_CONFIDENCE).count();
        long failed = auditLog.stream().filter(r -> r.getStatus() == HealingStatus.FAILED).count();

        double successRate = total > 0 ? (double) success / total * 100.0 : 0.0;
        double avgLatency = auditLog.stream().mapToLong(HealingActionRecord::getExecutionTimeMs).average().orElse(0.0);
        long cacheHits = auditLog.stream().filter(r -> r.getDiagnosisLatencyMs() == 0 && r.getStatus() == HealingStatus.SUCCESS).count();
        double cacheHitRate = total > 0 ? (double) cacheHits / total * 100.0 : 0.0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalHealingAttempts", total);
        summary.put("successfulHeals", success);
        summary.put("blockedByPolicy", blockedPolicy);
        summary.put("blockedByCircuitBreaker", blockedCb);
        summary.put("skippedLowConfidence", lowConfidence);
        summary.put("failedHeals", failed);
        summary.put("successRatePercentage", Math.round(successRate * 10.0) / 10.0);
        summary.put("averageLatencyMs", Math.round(avgLatency * 10.0) / 10.0);
        summary.put("cacheHitCount", cacheHits);
        summary.put("cacheHitRatePercentage", Math.round(cacheHitRate * 10.0) / 10.0);

        return summary;
    }

    public void clear() {
        auditLog.clear();
        actionIndex.clear();
        if (actionRepository != null) {
            try {
                actionRepository.deleteAll();
            } catch (Exception e) {
                log.warn("[Healing Audit] Failed to clear persisted actions: {}", e.getMessage());
            }
        }
    }
}

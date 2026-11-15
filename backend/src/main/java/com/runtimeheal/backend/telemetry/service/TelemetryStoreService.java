package com.runtimeheal.backend.telemetry.service;

import com.runtimeheal.backend.telemetry.dto.AgentTelemetryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.runtimeheal.backend.persistence.entity.IncidentEntity;
import com.runtimeheal.backend.persistence.repository.IncidentRepository;
import org.springframework.data.domain.PageRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory and persistent repository storing failure events captured by the ByteBuddy agent.
 */
@Service
public class TelemetryStoreService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryStoreService.class);
    private static final int MAX_INCIDENTS = 1000;

    private final IncidentRepository incidentRepository;
    private final List<AgentTelemetryEvent> incidents = new CopyOnWriteArrayList<>();
    private final Map<String, AgentTelemetryEvent> incidentIndex = new ConcurrentHashMap<>();

    public TelemetryStoreService() {
        this(null);
    }

    @Autowired
    public TelemetryStoreService(@Autowired(required = false) IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @PostConstruct
    public void init() {
        if (incidentRepository != null) {
            try {
                List<IncidentEntity> saved = incidentRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, MAX_INCIDENTS));
                // Add in chronological order to memory
                List<IncidentEntity> chronological = new ArrayList<>(saved);
                Collections.reverse(chronological);
                for (IncidentEntity entity : chronological) {
                    AgentTelemetryEvent dto = entity.toDto();
                    incidents.add(dto);
                    incidentIndex.put(dto.getIncidentId(), dto);
                }
                log.info("[Telemetry Store] Hydrated {} persisted incidents from database", saved.size());
            } catch (Exception e) {
                log.warn("[Telemetry Store] Could not hydrate incidents from database: {}", e.getMessage());
            }
        }
    }

    /**
     * Records an intercepted failure event.
     */
    public synchronized AgentTelemetryEvent recordIncident(AgentTelemetryEvent event) {
        if (event == null || event.getIncidentId() == null) {
            log.warn("Attempted to record null or invalid telemetry event");
            return null;
        }

        if (incidents.size() >= MAX_INCIDENTS) {
            AgentTelemetryEvent oldest = incidents.remove(0);
            if (oldest != null) {
                incidentIndex.remove(oldest.getIncidentId());
            }
        }

        incidents.add(event);
        incidentIndex.put(event.getIncidentId(), event);

        if (incidentRepository != null) {
            try {
                incidentRepository.save(IncidentEntity.fromDto(event));
            } catch (Exception e) {
                log.warn("[Telemetry Store] Failed to persist incident {}: {}", event.getIncidentId(), e.getMessage());
            }
        }

        log.info("[Telemetry Store] Logged incident {} -> {} in {}.{}()",
                event.getIncidentId(), event.getExceptionType(), event.getClassName(), event.getMethodName());

        return event;
    }

    public List<AgentTelemetryEvent> getAllIncidents() {
        List<AgentTelemetryEvent> reversed = new ArrayList<>(incidents);
        Collections.reverse(reversed);
        return Collections.unmodifiableList(reversed);
    }

    public Optional<AgentTelemetryEvent> getIncidentById(String incidentId) {
        return Optional.ofNullable(incidentIndex.get(incidentId));
    }

    public int getTotalIncidentCount() {
        return incidents.size();
    }

    public Map<String, Long> getExceptionDistribution() {
        Map<String, Long> distribution = new HashMap<>();
        for (AgentTelemetryEvent event : incidents) {
            distribution.merge(event.getExceptionType(), 1L, Long::sum);
        }
        return distribution;
    }

    public void clear() {
        incidents.clear();
        incidentIndex.clear();
        if (incidentRepository != null) {
            try {
                incidentRepository.deleteAll();
            } catch (Exception e) {
                log.warn("[Telemetry Store] Failed to clear persisted incidents: {}", e.getMessage());
            }
        }
    }
}

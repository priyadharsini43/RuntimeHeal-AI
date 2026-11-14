package com.runtimeheal.backend.recovery.service;

import com.runtimeheal.agent.recovery.RecoveryCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

/**
 * Bootstrap service that registers the SelfHealingEngine with the ByteBuddy Java Agent
 * RecoveryCoordinator upon Spring Boot container readiness.
 */
@Service
public class SelfHealingBootstrapService implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(SelfHealingBootstrapService.class);

    private final SelfHealingEngine healingEngine;

    public SelfHealingBootstrapService(SelfHealingEngine healingEngine) {
        this.healingEngine = healingEngine;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("[SelfHealingBootstrap] Registering SelfHealingEngine with agent RecoveryCoordinator...");
        RecoveryCoordinator.registerHandler(healingEngine);
        log.info("[SelfHealingBootstrap] SelfHealingEngine registered successfully. Dynamic healing active.");
    }
}

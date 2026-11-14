package com.runtimeheal.agent.recovery;

import com.runtimeheal.agent.telemetry.RecursionGuard;

import java.lang.reflect.Method;

/**
 * Global coordinator in the agent that mediates between ByteBuddy Advice and the registered RecoveryHandler.
 */
public class RecoveryCoordinator {

    private static volatile RecoveryHandler recoveryHandler;
    private static volatile boolean enabled = true;

    public static void setEnabled(boolean isEnabled) {
        enabled = isEnabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void registerHandler(RecoveryHandler handler) {
        recoveryHandler = handler;
        System.out.println("[RuntimeHeal Agent] RecoveryHandler registered successfully.");
    }

    public static void unregisterHandler() {
        recoveryHandler = null;
    }

    public static boolean hasHandler() {
        return recoveryHandler != null;
    }

    public static RecoveryDecision evaluateRecovery(
            String className,
            String methodName,
            String methodSignature,
            String returnTypeName,
            Object target,
            Method method,
            Object[] args,
            Throwable thrown,
            String incidentId) {

        if (!enabled) {
            return RecoveryDecision.noAction(null, "RecoveryCoordinator is disabled");
        }

        RecoveryHandler handler = recoveryHandler;
        if (handler == null) {
            return RecoveryDecision.noAction(null, "No RecoveryHandler registered");
        }

        if (RecursionGuard.isGuarded()) {
            return RecoveryDecision.noAction(null, "Recursion guard active");
        }

        try {
            RecursionGuard.enter();
            return handler.handleRecovery(
                    className, methodName, methodSignature, returnTypeName,
                    target, method, args, thrown, incidentId
            );
        } catch (Throwable t) {
            System.err.println("[RuntimeHeal Agent] Error during recovery evaluation: " + t.getMessage());
            return RecoveryDecision.noAction(null, "Evaluation error: " + t.getMessage());
        } finally {
            RecursionGuard.exit();
        }
    }
}

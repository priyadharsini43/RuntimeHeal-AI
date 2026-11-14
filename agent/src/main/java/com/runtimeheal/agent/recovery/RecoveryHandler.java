package com.runtimeheal.agent.recovery;

import java.lang.reflect.Method;

/**
 * Functional SPI implemented by the recovery engine to evaluate healing actions.
 */
@FunctionalInterface
public interface RecoveryHandler {

    /**
     * Evaluates whether a failed method can be safely healed.
     *
     * @param className       the target class name
     * @param methodName      the target method name
     * @param methodSignature the method signature/descriptor
     * @param returnTypeName  the return type name (e.g., int, void, java.lang.String)
     * @param target          the instance target (null if static)
     * @param method          the reflective Method object
     * @param args            the arguments passed to the method
     * @param thrown          the unhandled exception
     * @param incidentId      the correlation incident ID
     * @return a RecoveryDecision indicating if the exception should be suppressed and what to return
     */
    RecoveryDecision handleRecovery(
            String className,
            String methodName,
            String methodSignature,
            String returnTypeName,
            Object target,
            Method method,
            Object[] args,
            Throwable thrown,
            String incidentId
    );
}

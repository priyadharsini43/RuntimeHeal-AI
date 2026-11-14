package com.runtimeheal.agent.advice;

import com.runtimeheal.agent.recovery.RecoveryCoordinator;
import com.runtimeheal.agent.recovery.RecoveryDecision;
import com.runtimeheal.agent.telemetry.CapturedFailureEvent;
import com.runtimeheal.agent.telemetry.TelemetryDispatcher;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;

/**
 * ByteBuddy Advice that intercepts method execution exits.
 * If an unhandled Throwable occurred, it extracts telemetry and checks with the RecoveryCoordinator
 * to determine whether the exception should be healed (suppressed with a safe fallback value)
 * or propagated normally.
 */
public class ExceptionCaptureAdvice {

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onExit(
            @Advice.Origin("#t") String className,
            @Advice.Origin("#m") String methodName,
            @Advice.Origin("#s") String methodSignature,
            @Advice.Origin("#r") String returnTypeName,
            @Advice.Origin Method method,
            @Advice.This(optional = true) Object target,
            @Advice.AllArguments Object[] args,
            @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned,
            @Advice.Thrown(readOnly = false) Throwable thrown) {

        if (thrown != null) {
            CapturedFailureEvent event = TelemetryDispatcher.captureAndDispatch(className, methodName, methodSignature, args, thrown);
            String incidentId = event != null ? event.getIncidentId() : null;

            RecoveryDecision decision = RecoveryCoordinator.evaluateRecovery(
                    className, methodName, methodSignature, returnTypeName,
                    target, method, args, thrown, incidentId
            );

            if (decision != null && decision.isHealed()) {
                // Suppress the exception
                thrown = null;
                // Supply fallback return value if not void
                if (!"void".equalsIgnoreCase(returnTypeName) && !"V".equals(returnTypeName)) {
                    returned = decision.getFallbackValue();
                }
            }
        }
    }
}

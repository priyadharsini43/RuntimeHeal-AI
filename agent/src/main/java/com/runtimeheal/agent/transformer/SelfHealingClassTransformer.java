package com.runtimeheal.agent.transformer;

import com.runtimeheal.agent.advice.ExceptionCaptureAdvice;
import com.runtimeheal.agent.annotation.SelfHealingTarget;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Builds the ByteBuddy AgentBuilder configuration for targeted method instrumentation.
 */
public class SelfHealingClassTransformer {

    private static final String DEFAULT_TARGET_PACKAGE = "com.runtimeheal.backend.sample.service";

    public static AgentBuilder configure(AgentBuilder agentBuilder) {
        return configure(agentBuilder, DEFAULT_TARGET_PACKAGE);
    }

    public static AgentBuilder configure(AgentBuilder agentBuilder, String targetPackagePrefix) {
        ElementMatcher.Junction<TypeDescription> typeMatcher = (nameStartsWith(targetPackagePrefix)
                .and(not(nameEndsWith("Controller")))
                .and(not(nameEndsWith("ExceptionHandler")))
                .and(not(nameEndsWith("Response")))
                .and(not(nameEndsWith("Request"))))
                .or(isAnnotatedWith(SelfHealingTarget.class));

        return agentBuilder
                .type(typeMatcher)
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                    System.out.println("[RuntimeHeal Agent] Instrumenting target type: " + typeDescription.getName());

                    return builder.visit(
                            Advice.to(ExceptionCaptureAdvice.class)
                                    .on(isMethod()
                                            .and(not(isAbstract()))
                                            .and(not(isNative()))
                                            .and(not(isSynthetic()))
                                            .and(not(isConstructor()))
                                            .and(not(named("toString")))
                                            .and(not(named("hashCode")))
                                            .and(not(named("equals"))))
                    );
                });
    }
}

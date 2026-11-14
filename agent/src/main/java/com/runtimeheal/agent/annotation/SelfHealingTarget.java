package com.runtimeheal.agent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opt-in annotation to explicitly mark classes or methods for ByteBuddy self-healing instrumentation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SelfHealingTarget {

    /**
     * Optional scenario identifier for telemetry and grouping.
     */
    String scenario() default "";

    /**
     * Whether the targeted operation is safe and idempotent for automated retries.
     */
    boolean idempotent() default false;
}

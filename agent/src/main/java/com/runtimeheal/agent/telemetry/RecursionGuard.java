package com.runtimeheal.agent.telemetry;

/**
 * ThreadLocal recursion guard preventing infinite interception loops.
 * Protects against scenarios where telemetry extraction itself throws an exception.
 */
public final class RecursionGuard {

    private static final ThreadLocal<Boolean> IN_PROGRESS = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private RecursionGuard() {
    }

    public static boolean isGuarded() {
        return Boolean.TRUE.equals(IN_PROGRESS.get());
    }

    public static void enter() {
        IN_PROGRESS.set(Boolean.TRUE);
    }

    public static void exit() {
        IN_PROGRESS.remove();
    }
}

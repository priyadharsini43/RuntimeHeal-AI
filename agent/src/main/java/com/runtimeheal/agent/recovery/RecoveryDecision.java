package com.runtimeheal.agent.recovery;

/**
 * Outcome of a healing evaluation performed by the RecoveryCoordinator.
 */
public class RecoveryDecision {

    private final boolean healed;
    private final Object fallbackValue;
    private final String actionId;
    private final String strategy;
    private final String reason;

    private RecoveryDecision(boolean healed, Object fallbackValue, String actionId, String strategy, String reason) {
        this.healed = healed;
        this.fallbackValue = fallbackValue;
        this.actionId = actionId;
        this.strategy = strategy;
        this.reason = reason;
    }

    public static RecoveryDecision heal(Object fallbackValue, String actionId, String strategy, String reason) {
        return new RecoveryDecision(true, fallbackValue, actionId, strategy, reason);
    }

    public static RecoveryDecision noAction(String actionId, String reason) {
        return new RecoveryDecision(false, null, actionId, "NO_ACTION", reason);
    }

    public boolean isHealed() {
        return healed;
    }

    public Object getFallbackValue() {
        return fallbackValue;
    }

    public String getActionId() {
        return actionId;
    }

    public String getStrategy() {
        return strategy;
    }

    public String getReason() {
        return reason;
    }
}

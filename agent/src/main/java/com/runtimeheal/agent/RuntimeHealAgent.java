package com.runtimeheal.agent;

import com.runtimeheal.agent.transformer.SelfHealingClassTransformer;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

/**
 * Entry point for the RuntimeHeal ByteBuddy Java Agent.
 * Supports static premain attachment (-javaagent) and dynamic runtime attach.
 */
public class RuntimeHealAgent {

    private static volatile Instrumentation instrumentation;
    private static volatile boolean initialized = false;

    /**
     * JVM startup hook (-javaagent:path/to/runtimeheal-agent.jar)
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        initAgent(agentArgs, inst);
    }

    /**
     * Dynamic runtime attach hook (programmatic attachment)
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        initAgent(agentArgs, inst);
    }

    /**
     * Convenience programmatic installer for in-process testing.
     */
    public static synchronized boolean install() {
        if (initialized) {
            return true;
        }
        try {
            Instrumentation inst = ByteBuddyAgent.install();
            initAgent(null, inst);
            return true;
        } catch (Throwable t) {
            System.err.println("[RuntimeHeal Agent] Dynamic attach failed: " + t.getMessage());
            return false;
        }
    }

    private static synchronized void initAgent(String agentArgs, Instrumentation inst) {
        if (initialized) {
            return;
        }
        instrumentation = inst;

        System.out.println("=================================================");
        System.out.println(">>> RuntimeHeal AI - ByteBuddy Java Agent Active <<<");
        System.out.println(">>> Detect. Understand. Recover.              <<<");
        System.out.println("=================================================");

        AgentBuilder agentBuilder = new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE);

        SelfHealingClassTransformer.configure(agentBuilder).installOn(inst);
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}

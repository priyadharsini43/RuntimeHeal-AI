# RuntimeHeal AI — Viva & Project Defense Guide

> **Project Title:** RuntimeHeal AI — An AI-Driven Runtime Self-Healing Framework for Java Applications Using ByteBuddy and Local Large Language Models  
> **Tagline:** Detect. Understand. Recover.  
> **Academic Level:** Final-Year Engineering Project Defense

---

## 1. Problem Statement & Motivation

### The Industry Challenge
- **Runtime Exceptions Cause Severe Downtime**: In distributed Java microservices, runtime exceptions (e.g. unexpected division by zero, null dereferences, transient downstream latency) cause cascading failures and SLA breaches.
- **High Mean Time to Detect and Repair (MTTD / MTTR)**: Traditional monitoring (APMs, logs) is passive. Human operators must triage logs, debug stack traces, write patches, review PRs, and redeploy containers—often taking hours.
- **Cloud LLM Limitations in Production**: Relying on external proprietary LLMs (e.g. OpenAI GPT-4, Claude) introduces severe privacy risks (sending proprietary stack traces/PII externally), recurring API costs, network latency, and availability dependencies.
- **Uncontrolled AI Execution Risks**: Raw AI models hallucinate arbitrary code or unsafe shell commands. Autonomous recovery must be bounded, deterministic, and sandboxed to prevent data corruption.

### The RuntimeHeal AI Solution
RuntimeHeal AI introduces an intelligent, autonomous, in-process self-healing framework that combines:
1. **ByteBuddy Runtime Instrumentation**: Non-invasive bytecode manipulation to intercept exceptions before they crash requests.
2. **Local Edge LLM Inference**: Private, offline root-cause analysis using a local Ollama model (`qwen2.5:3b`) running entirely on-premise.
3. **Safety Guardrails & Allowlist Matrix**: Enforcing transactional denylists, sliding-window circuit breakers, and confidence thresholds to eliminate rogue AI behavior.
4. **Bounded Recovery Engine**: Dynamic exception suppression and type-safe fallback injection or bounded idempotent retries.

---

## 2. Core Architectural Pillars

```
+-----------------------------------------------------------------------------------+
|                            RUNTIME APPLICATION (JVM)                              |
|                                                                                   |
|   +--------------------------+           +------------------------------------+   |
|   |   Application Services   |           |    ByteBuddy Java Agent Advice     |   |
|   |   (OrderProcessing, etc) | <=======> |   (@Advice.OnMethodExit)           |   |
|   +--------------------------+           +------------------------------------+   |
+-------------------------------------------------------│---------------------------+
                                                        │ (Intercepts Exception)
                                                        ▼
+-----------------------------------------------------------------------------------+
|                        RUNTIMEHEAL AI BACKEND ENGINE                              |
|                                                                                   |
|   +------------------------------------+   +----------------------------------+   |
|   |     TelemetryDispatcher            |   |     TelemetryStoreService        |   |
|   |     (Captures FailureContext)      |──>|     (In-Memory Telemetry Ring)   |   |
|   +------------------------------------+   +----------------------------------+   |
|                                                                                   |
|   +------------------------------------+   +----------------------------------+   |
|   |     LlmDiagnosticService           |   |     Local Ollama LLM             |   |
|   |     (Prompt Builder + Fallback)    |<─>|     (qwen2.5:3b @ :11434)        |   |
|   +------------------------------------+   +----------------------------------+   |
|                     │ (Diagnostic Report)                                         |
|                     ▼                                                             |
|   +---------------------------------------------------------------------------+   |
|   |     SelfHealingEngine (Policy Guardrails & Allowlist Matrix)              |   |
|   |     - Master Switch: Enabled?                                             |   |
|   |     - Transactional Denylist: validateAndProcessTransfer?                 |   |
|   |     - Method Circuit Breaker: < 5 failures per 60s window?                |   |
|   |     - LLM Confidence Score: >= 0.70?                                      |   |
|   +---------------------------------------------------------------------------+   |
|                     │                                                             |
|                     ├──> RETURN_FALLBACK: Injects TypeSafeFallbackProvider        |
|                     ├──> RETRY: In-process reflective retry with backoff          |
|                     └──> NO_ACTION: Safe fail-through for denylisted methods      |
|                                                                                   |
|   +------------------------------------+   +----------------------------------+   |
|   |     HealingAuditService            |   |     Embedded Dashboard           |   |
|   |     (Full Telemetry Audit Trail)   |──>|     (http://localhost:8080/)     |   |
|   +------------------------------------+   +----------------------------------+   |
+-----------------------------------------------------------------------------------+
```

---

## 3. Deep-Dive Technical Mechanics

### A. How ByteBuddy Intercepts & Suppresses Exceptions
- In `ExceptionCaptureAdvice.java`, ByteBuddy's `@Advice.OnMethodExit` annotation intercepts method returns and exits.
- By configuring `@Advice.Thrown(readOnly = false) Throwable thrown` and `@Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned`:
  1. The agent detects `thrown != null`.
  2. Dispatches `FailureContext` telemetry to the backend.
  3. Evaluates recovery via `RecoveryCoordinator.evaluateRecovery(...)`.
  4. If `decision.isHealed()` is true:
     - Sets `thrown = null;` $\rightarrow$ The JVM cancels the unhandled exception throw!
     - Sets `returned = decision.getFallbackValue();` $\rightarrow$ The JVM returns the type-safe fallback directly to the calling controller!
  5. If `decision.isHealed()` is false:
     - The original exception propagates unhindered to standard exception handlers.

### B. Why ByteBuddy Instead of Spring AOP?
| Feature | ByteBuddy Java Agent | Traditional Spring AOP |
|---|---|---|
| **Mechanism** | In-process JVM Bytecode Instrumentation | Dynamic Proxies / CGLIB |
| **Scope** | Any Java class, static methods, internal calls | Only Spring-managed public bean calls |
| **Internal Method Calls** | Fully intercepted (`this.method()`) | Bypassed by dynamic proxy wrapper |
| **Overhead** | Microsecond level (JIT optimized bytecode) | High (reflection & proxy invocation chain) |
| **Portability** | Standalone agent (`-javaagent`) | Bound to Spring Framework lifecycle |

### C. Prompt Engineering & JSON Enforcement with Local Ollama
- **Model**: `qwen2.5:3b` running on local Ollama (`http://localhost:11434`).
- **Format Constraint**: `format: "json"` enforced at HTTP request level.
- **Strict Strategy Bounds**: The prompt explicitly forbids freeform code generation, restricting recovery strategies to bounded tokens:
  `[NO_ACTION, RETURN_FALLBACK, RETRY, SKIP_OPERATION]`.
- **Deterministic Heuristic Fallback**: If Ollama is offline or latency breaches timeout, the engine immediately transitions to rule-based fallback heuristics with 0 ms overhead.

### D. Safety Guardrails: Why the Framework Won't Break Banking Apps
1. **Transactional Method Denylist**: Critical transactional methods (e.g. `validateAndProcessTransfer`, `executeGatewayCall`) are denylisted. Automated recovery will NEVER inject dummy balances or tamper with financial operations.
2. **Sliding-Window Circuit Breakers**: Prevents infinite recovery loops. If a method fails $\ge 5$ times in 60 seconds, the circuit trips to `OPEN`, immediately cutting off automated healing and alerting human operators.
3. **Confidence Gating**: The LLM must report $\ge 0.70$ confidence. Ambiguous errors default to `NO_ACTION`.
4. **Recursion Guard**: ThreadLocal guard prevents recursive interception loops during telemetry logging or reflective retries.

---

## 4. Expected Viva Questions & Model Answers

### Q1: How does your system guarantee that the LLM will not hallucinate dangerous code?
> **Answer:** "Our framework does NOT allow the LLM to generate executable code or shell scripts. Instead, we use an architectural pattern called **Bounded Strategy Tokens**. The LLM is restricted via JSON schema to selecting from four predefined, audited strategies: `RETURN_FALLBACK`, `RETRY`, `SKIP_OPERATION`, and `NO_ACTION`. The actual execution logic is strictly implemented in verified Java code inside the `SelfHealingEngine`, which uses pre-compiled type-safe fallback providers and reflective retries."

### Q2: What happens if the Ollama service crashes or times out?
> **Answer:** "RuntimeHeal AI includes a resilient **Rule-Based Heuristic Fallback**. The `OllamaClient` continuously monitors service availability. If Ollama is offline, unreachable, or times out, the `LlmDiagnosticService` instantly activates deterministic heuristic rules based on standard Java exception hierarchies (e.g. mapping `ArithmeticException` to `RETURN_FALLBACK` with 0 ms latency). This ensures zero application downtime regardless of AI availability."

### Q3: How do you handle financial or sensitive transactions where fallback values would be dangerous?
> **Answer:** "We enforce a **Transactional Method Denylist** configured via `runtimeheal.healing.denylist`. Methods such as `validateAndProcessTransfer` are prohibited from automated fallback tampering. When an exception occurs in a denylisted method, the engine halts with status `BLOCKED_POLICY`, records an audit event, and allows the original exception to propagate cleanly to produce standard HTTP 400/500 responses."

### Q4: How is the exception suppressed at the JVM level?
> **Answer:** "We leverage ByteBuddy's `@Advice.OnMethodExit` mechanism with writable parameters: `@Advice.Thrown(readOnly = false) Throwable thrown` and `@Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned`. By assigning `thrown = null`, ByteBuddy's generated bytecode instructs the JVM stack frame to discard the exception object and instead jump to the normal method return instruction with the value assigned to `returned`."

### Q5: How do you prevent infinite self-healing loops if an underlying database is permanently down?
> **Answer:** "We implement a **Sliding-Window Circuit Breaker** per method. By default, if a method fails 5 times within a 60-second window, the circuit breaker transitions from `CLOSED` to `OPEN`. In the `OPEN` state, the recovery engine rejects further healing attempts with status `BLOCKED_CIRCUIT_BREAKER`, letting the exception surface to prevent resource exhaustion and alerting site reliability engineers."

---

## 5. Live Demonstration Checklist

1. **Pre-requisite Check**:
   - Verify Ollama: `curl http://localhost:11434/api/tags`
   - Start application: `.\run.bat`
2. **Dashboard Verification**:
   - Open browser to `http://localhost:8080/`.
   - Show the evaluator the Agent badge (`ACTIVE (PREMAIN)`), Ollama badge (`CONNECTED (qwen2.5:3b)`), and KPI cards.
3. **Automated Demo**:
   - Run `.\scripts\demo.ps1` in a PowerShell terminal.
   - Walk through Scenarios A, B, and C as the green `[PASS]` badges appear.
4. **Interactive UI Fault Injection**:
   - Click "Arithmetic Fault" button in the dashboard $\rightarrow$ watch the live table update with `discountRatio: 0` and Status: `SUCCESS`.
   - Click "Transactional Denylist" button $\rightarrow$ watch the audit table record `BLOCKED_POLICY`.
   - Adjust the confidence threshold slider and demonstrate dynamic policy updates.

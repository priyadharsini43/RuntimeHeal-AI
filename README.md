# RuntimeHeal AI

> **Tagline:** Detect. Understand. Recover.  
> **Project Title:** An AI-Driven Runtime Self-Healing Framework for Java Applications Using ByteBuddy and Local Large Language Models.

---

## Overview

RuntimeHeal AI is an intelligent runtime self-healing framework for Java applications. It instruments bytecode at runtime using ByteBuddy to intercept runtime exceptions, constructs structured failure telemetry, queries a locally hosted Large Language Model (via Ollama) for root-cause diagnosis and structured recovery actions, validates suggestions against an allowlist safety matrix, and executes safe recovery actions (such as safe idempotent retries, fallback returns, or controlled skip operations).

---

## Project Structure

```
RuntimeSelfHealing/
├── backend/          # Spring Boot 3 REST API & Self-Healing Engine
│   ├── src/
│   │   ├── main/java/com/runtimeheal/backend/
│   │   │   ├── health/         # System telemetry & Actuator
│   │   │   └── sample/         # Sample application & controlled exception triggers
│   │   └── test/java/com/runtimeheal/backend/
│   └── pom.xml
├── agent/            # ByteBuddy Java Instrumentation Agent (Phase 3)
├── frontend/         # React + Vite + Tailwind CSS Observability Dashboard (Phase 8)
├── docs/             # Technical specifications and architectural diagrams
├── results/          # Evaluation benchmarks and metrics
├── logs/             # Application and agent runtime logs
└── README.md
```

---

## Progress Status

- [x] **Phase 1: Project Foundation** — Spring Boot 3.3.5 backend, `/api/health`, Actuator probes, port 8080, JDK 17 baseline.
- [x] **Phase 2: Sample Application & Exceptions** — Isolated services, controlled fault triggers, structured error payloads, unit and integration tests.
- [x] **Phase 3: ByteBuddy Java Agent** — Shaded runtime instrumentation agent, premain & dynamic attach, telemetry dispatch.
- [x] **Phase 4: Local Ollama LLM Diagnostics** — Local Ollama `qwen2.5:3b` integration, JSON-enforced schema, and heuristic fallback.
- [x] **Phase 5: Bounded Recovery Engine & Safe Self-Healing** — Dynamic exception suppression, type-safe fallbacks, sliding-window circuit breakers, transactional denylist, and audit trails.
- [x] **Phase 6: Real-Time Observability Dashboard & Demo Suite** — Zero-dependency dark-themed SPA dashboard at `/`, cross-platform demo scripts (`demo.ps1`, `demo.sh`), and viva defense documentation.
- [ ] **Phase 7: Evaluation & Benchmarks** — Performance, detection, recovery benchmarks, and latency profiling.
- [ ] **Phase 8: Final Documentation & Demonstration** — Final report, demonstration assets, and presentation.

---

## Prerequisites

- **Java**: JDK 17 LTS or JDK 21
- **Maven**: Apache Maven 3.9+
- **Local LLM**: Ollama (for Phase 6+)
- **Node.js**: v18+ (for Phase 8 React frontend)

---

## Getting Started: Backend

### 1. Build and Test
```bash
cd backend
mvn clean test
```

### 2. Package
```bash
mvn package -DskipTests
```

### 3. Run the Application
```bash
java -jar target/runtimeheal-backend-0.0.1-SNAPSHOT.jar
```
Or via Maven:
```bash
mvn spring-boot:run
```

---

## REST API Endpoints

### System Telemetry (Phase 1)
| Method | Endpoint | Description | Expected Status |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Service health, JVM version, uptime | `200 OK` |
| `GET` | `/actuator/health` | Spring Boot Actuator probe | `200 OK` |

### Controlled Sample Exception Scenarios (Phase 2)

#### 1. Scenario Catalog
```bash
curl -i http://localhost:8080/api/sample/scenarios
```
Returns metadata for all available fault injection scenarios for the dashboard Test Lab.

#### 2. ArithmeticException (Division by Zero)
```bash
# Triggers ArithmeticException (500 Internal Server Error)
curl -i "http://localhost:8080/api/sample/arithmetic?total=100&divisor=0"

# Normal execution (200 OK)
curl -i "http://localhost:8080/api/sample/arithmetic?total=100&divisor=5"
```

#### 3. NullPointerException (Uninitialized Profile / Missing Preferences)
```bash
# Triggers NullPointerException (500 Internal Server Error)
curl -i "http://localhost:8080/api/sample/npe?accountId=UNKNOWN-999"

# Normal execution (200 OK)
curl -i "http://localhost:8080/api/sample/npe?accountId=ACC-100"
```

#### 4. IllegalArgumentException (Business Validation Failure)
```bash
# Triggers IllegalArgumentException (400 Bad Request)
curl -i -X POST http://localhost:8080/api/sample/illegal-argument \
  -H "Content-Type: application/json" \
  -d '{"accountId": "ACC-100", "amount": -50.00, "currency": "USD"}'

# Normal execution (200 OK)
curl -i -X POST http://localhost:8080/api/sample/illegal-argument \
  -H "Content-Type: application/json" \
  -d '{"accountId": "ACC-100", "amount": 75.00, "currency": "USD"}'
```

#### 5. Simulated Gateway Timeout (Transient Latency)
```bash
# Triggers SimulatedTimeoutException (504 Gateway Timeout)
curl -i "http://localhost:8080/api/sample/timeout?simulateSlow=true&delayMillis=3000"

# Normal execution (200 OK)
curl -i "http://localhost:8080/api/sample/timeout?simulateSlow=false"
```

---

## Structured Error Response Format

When any exception occurs, the system responds with a structured JSON schema capturing incident telemetry:

```json
{
  "incidentId": "INC-ARITH-3A9B8F21",
  "timestamp": "2026-11-02T14:05:00Z",
  "status": 500,
  "error": "Internal Arithmetic Failure",
  "exceptionType": "java.lang.ArithmeticException",
  "message": "/ by zero in discount allocation",
  "path": "/api/sample/arithmetic"
}
```

---

## Phase 3: ByteBuddy Java Agent Telemetry

The application attaches a ByteBuddy agent (via `-javaagent` or dynamic attach) to monitor method exits non-invasively:

- `GET /api/agent/status` — Reports agent active status, attach mode (`PREMAIN` or `DYNAMIC_ATTACH`), and target package.
- `GET /api/telemetry/incidents` — Lists captured `FailureContext` incidents intercepted by the agent.
- `GET /api/telemetry/summary` — Returns total incidents and distribution across exception types.

---

## Phase 4: Local Ollama LLM Diagnostics (`qwen2.5:3b`)

RuntimeHeal AI integrates with a local Ollama instance (`http://localhost:11434`) running `qwen2.5:3b` to diagnose intercepted runtime failures without cloud dependencies.

### Diagnostic Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/diagnostics/health` | Checks Ollama reachability and configured model status. |
| `POST` | `/api/diagnostics/analyze/{incidentId}` | Generates an LLM diagnostic report for a captured incident. |
| `POST` | `/api/diagnostics/analyze` | Analyzes an ad-hoc FailureContext payload. |
| `GET` | `/api/diagnostics/{incidentId}` | Retrieves a cached diagnostic report. |
| `GET` | `/api/diagnostics/history` | Lists all generated diagnostic reports. |

### Sample Diagnosis Output (`POST /api/diagnostics/analyze/{incidentId}`)

```json
{
  "incidentId": "INC-AGENT-06AFCF9B",
  "timestamp": "2026-11-05T05:45:48.923414400Z",
  "rootCause": "The division operation in the calculateDiscountRatio method attempts to divide by zero, leading to an ArithmeticException.",
  "explanation": "The exception occurs because the calculateDiscountRatio method in the OrderProcessingService class is designed to perform a division operation, but the method does not validate the divisor before performing the division.",
  "severity": "MEDIUM",
  "confidenceScore": 0.85,
  "suggestedRecoveryStrategy": "RETRY",
  "reasonForSuggestion": "Retrying the operation with validated input arguments is a safe strategy because it addresses the root cause of the exception by ensuring that the divisor is not zero before performing the division operation.",
  "source": "LOCAL_OLLAMA_QWEN2.5:3B",
  "latencyMillis": 65531
}
```

---

## Phase 5: Bounded Recovery Engine & Safe Self-Healing

The self-healing execution engine connects ByteBuddy runtime exception interception with safety guardrails and bounded recovery execution:

### Recovery Strategies
- **`RETURN_FALLBACK`**: Generates and returns type-safe defaults based on target method return type (e.g., `0` for numeric primitives, `""` or `"DEFAULT_FALLBACK"` for Strings, empty Collections, `Optional.empty()`).
- **`RETRY`**: Safely re-attempts transient operations via reflection with exponential backoff up to configurable `maxRetries`.
- **`SKIP_OPERATION`**: Safely bypasses non-critical void method failures while recording execution audit.
- **`NO_ACTION`**: Fails safe without intervention when recovery would risk transactional data integrity.

### Safety Guardrails
- **Transactional Denylist**: Prohibits automated recovery for critical financial methods (e.g. `validateAndProcessTransfer`, `executeGatewayCall`), ensuring exceptions propagate normally.
- **Sliding-Window Circuit Breakers**: Method-level circuit breakers trip to `OPEN` state after exceeding failure thresholds (default: 5 failures in 60s) to prevent infinite recovery loops.
- **Confidence Threshold**: Bypasses automated recovery if LLM diagnostic confidence is below `minConfidence` (default: 0.70).

### Self-Healing Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/healing/actions` | Lists all recorded self-healing execution audit records. |
| `GET` | `/api/healing/actions/{actionId}` | Retrieves details for a specific healing action by ID. |
| `GET` | `/api/healing/summary` | Returns recovery metrics (total attempts, successful heals, blocked by policy/circuit breaker, success rate). |
| `GET` | `/api/healing/policy` | Inspects active safety policies, thresholds, denylist, and circuit breaker states. |
| `POST` | `/api/healing/policy` | Dynamically updates runtime policy parameters (`enabled`, `minConfidence`, `denylist`, `maxRetries`). |
| `POST` | `/api/healing/circuit-breaker/reset` | Resets all or specific method circuit breakers to `CLOSED` state. |

---

## Phase 6: Real-Time Observability Dashboard & Demo Suite

RuntimeHeal AI includes an embedded, zero-dependency, dark-themed observability dashboard served directly from the Spring Boot application on port 8080:

### Dashboard Features (`http://localhost:8080/`)
- **System Telemetry Banner**: Displays real-time Java Agent health (`ACTIVE (PREMAIN)`), local Ollama LLM connectivity (`CONNECTED (qwen2.5:3b)`), and master healing toggle.
- **KPI Metrics Cards**: Total intercepted incidents, successful autonomous heals, recovery rate (%), blocked by policy, and circuit breaker status.
- **Interactive Fault Injector**: One-click execution of controlled runtime exceptions (`ArithmeticException`, `NullPointerException`, `IllegalArgumentException`, `SimulatedTimeoutException`).
- **Telemetry Stream & LLM Diagnostics**: Live table of intercepted incidents. Clicking any incident opens a modal displaying root-cause analysis, explanation, LLM confidence score, and stack trace.
- **Dynamic Policy Manager**: Interactive slider to adjust minimum LLM confidence thresholds, toggle master healing, view transactional denylists, and reset method circuit breakers.

### Quick Start & Demonstration

1. **Start Application with ByteBuddy Agent**:
   ```powershell
   # Windows launcher (auto-builds if needed):
   .\run.bat

   # Or manual command:
   java -javaagent:agent\target\runtimeheal-agent-0.0.1-SNAPSHOT.jar -jar backend\target\runtimeheal-backend-0.0.1-SNAPSHOT.jar
   ```

2. **Access the Dashboard**:
   Open your browser to: **`http://localhost:8080/`**

3. **Run Automated End-to-End Scenarios**:
   ```powershell
   # Run automated PowerShell demo suite:
   .\scripts\demo.ps1

   # Or on Linux/macOS/Git Bash:
   ./scripts/demo.sh
   ```

### Documentation & Defense Materials
- **[Viva Presentation & Defense Guide](docs/viva_presentation_guide.md)**: Problem statement, architectural pillars, deep-dive ByteBuddy mechanics, LLM prompting & JSON constraints, and FAQ.
- **[Architectural Overview](docs/architecture.md)**: End-to-end component diagrams and Mermaid sequence workflows.




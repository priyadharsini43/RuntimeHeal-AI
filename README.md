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
- **Local AI provider (optional)**: Ollama with `qwen2.5:3b`
- **Cloud AI provider (optional)**: A Groq API key and a model available on its no-cost allowance
- **Node.js**: v18+ (for Phase 8 React frontend)

## Free Cloud Deployment

The deployment files target a $0-only stack. They do not create resources or contain credentials:

```text
Browser -- HTTPS *.onrender.com --> Render Free Docker web service
                                     Spring Boot 3.3.5 / Java 17
                                     embedded dashboard + REST API
                                     -javaagent: RuntimeHeal ByteBuddy
                                     |                  |
                                     |                  +--> Groq free API allowance (optional)
                                     +--> Neon Free PostgreSQL

Local development can continue to use file-based H2 and Ollama/Qwen.
```

| Component | Selected tier | Cost target |
|---|---|---:|
| Render web service and its `onrender.com` HTTPS URL | Free | $0 |
| Neon PostgreSQL | Free | $0 |
| Groq API | No-cost allowance only; never enable paid billing | $0 |
| GitHub source repository and Render auto-deploy integration | Free | $0 |
| Custom domain, paid instance, paid database, and paid AI usage | Not used | $0 |

These are free-plan deployments, not an uptime guarantee. Render Free services can sleep and cold-start, Neon can suspend compute at plan limits, and Groq can reject requests at its rate limits. The container has no persistent disk; durable incidents, diagnoses, and healing audits use Neon. The in-memory caches and circuit-breaker state reset when Render restarts. If a provider is unconfigured or unavailable, the dashboard/API labels heuristic reports explicitly and shows the provider status; the app does not claim that a cloud AI call succeeded.

The Render instance is limited to 512 MB RAM. The Docker image caps Java heap at 192 MB, limits metaspace, uses Serial GC, and constrains server/database pools. ByteBuddy is attached at JVM startup by the Docker entrypoint; it is not silently replaced by dynamic attachment.

Free-tier terms and quotas can change. Verify the selected plan and $0 estimate on the [Render pricing page](https://render.com/pricing) and [free-service documentation](https://render.com/docs/free), [Neon pricing](https://neon.tech/pricing), [Groq model pricing](https://console.groq.com/docs/models) and [rate limits](https://console.groq.com/docs/rate-limits), and [GitHub plans](https://github.com/pricing) before creating anything. Some Groq models are explicitly token-priced. The `RUNTIMEHEAL_GROQ_FREE_ONLY` switch is a fail-closed configuration guard, not proof of account billing settings; if the console does not clearly show $0 for the chosen model and account, do not enable the switch or provide the key.

### Set up without payment or billing

1. Keep Render, Neon, GitHub, and Groq on their free plans. Do not add a payment method, enable billing, upgrade a plan, or configure a paid fallback. If any free allowance is unavailable, stop rather than accept a charge.
2. Push the `free-cloud-deploy` branch to a GitHub repository and connect that repository to Render. The included [`render.yaml`](./render.yaml) selects the Free plan, Dockerfile, HTTPS health check, and deploy-on-commit behavior.
3. In Neon, create a Free PostgreSQL project and copy its JDBC connection URL and credentials into the Render Blueprint prompts for `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`. Keep the connection URL's TLS option enabled.
4. In Groq, use a model only if the account explicitly shows it is available under the no-cost allowance and billing is disabled. Set `GROQ_API_KEY`, `RUNTIMEHEAL_GROQ_MODEL`, and `RUNTIMEHEAL_GROQ_FREE_ONLY=true` in Render's environment settings. The Groq client refuses to send requests unless that flag is explicitly true. Do not add billing; if no model is available for $0, leave Groq unconfigured and use local Ollama for local AI diagnosis.
5. Set a unique `SOC_OPERATOR_ID` and strong `SOC_OPERATOR_PASSCODE` in Render's secret environment settings. Do not put credentials in Git. Existing deployments can continue using `RUNTIMEHEAL_ADMIN_USER` and `RUNTIMEHEAL_ADMIN_PASSWORD`; these are supported as legacy fallbacks.
6. Import the Blueprint and verify the cost estimate is $0 before confirming. Render's generated `https://<service-name>.onrender.com` link is the single public URL. It does not exist until the owner connects accounts and creates the free service.

The public cloud instance cannot reach Ollama running on a laptop that is switched off. Ollama/Qwen remains available for local deployments; the cloud instance uses Groq only when its no-cost model and key are configured. No cloud resources have been created by preparing these files.

### Build the Docker image locally

From the project root:

```powershell
docker build -t runtimeheal:local .
docker run --rm -p 8080:8080 `
  -e SPRING_PROFILES_ACTIVE=production `
  -e SPRING_DATASOURCE_URL="<Neon JDBC URL>" `
  -e SPRING_DATASOURCE_USERNAME="<Neon username>" `
  -e SPRING_DATASOURCE_PASSWORD="<Neon password>" `
  -e SOC_OPERATOR_ID="<operator username>" `
  -e SOC_OPERATOR_PASSCODE="<operator passcode>" `
  runtimeheal:local
```

Add `GROQ_API_KEY` and `RUNTIMEHEAL_GROQ_MODEL` only when using a verified $0 Groq model. Local costs depend on the machine and are not cloud charges.

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
- **System Telemetry Banner**: Displays real-time Java Agent health (`ACTIVE (PREMAIN)`), the configured AI provider/model and its connection state, and the master healing toggle.
- **KPI Metrics Cards**: Total intercepted incidents, successful autonomous heals, recovery rate (%), blocked by policy, and circuit breaker status.
- **Interactive Fault Injector**: One-click execution of controlled runtime exceptions (`ArithmeticException`, `NullPointerException`, `IllegalArgumentException`, `SimulatedTimeoutException`).
- **Telemetry Stream & LLM Diagnostics**: Live table of intercepted incidents. Clicking any incident opens a modal displaying root-cause analysis, explanation, confidence, the actual diagnosis source (including heuristic fallback), and stack trace.
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




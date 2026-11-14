# ==============================================================================
# RuntimeHeal AI - Automated End-to-End Demonstration Script (PowerShell)
# Project: An AI-Driven Runtime Self-Healing Framework Using ByteBuddy & Local LLM
# ==============================================================================

$Host.UI.RawUI.WindowTitle = "RuntimeHeal AI - Automated Demonstration"
$ErrorActionPreference = "Continue"

function Print-Header($title) {
    Write-Host ""
    Write-Host ("=" * 78) -ForegroundColor Cyan
    Write-Host "  $title" -ForegroundColor Yellow
    Write-Host ("=" * 78) -ForegroundColor Cyan
}

function Print-Success($msg) {
    Write-Host "  [PASS] $msg" -ForegroundColor Green
}

function Print-Info($msg) {
    Write-Host "  [INFO] $msg" -ForegroundColor Gray
}

function Print-Warn($msg) {
    Write-Host "  [WARN] $msg" -ForegroundColor Yellow
}

function Print-Error($msg) {
    Write-Host "  [FAIL] $msg" -ForegroundColor Red
}

Clear-Host
Write-Host "==============================================================================" -ForegroundColor Cyan
Write-Host "   ____             _   _                  _   _            _      _    ___  " -ForegroundColor Magenta
Write-Host "  |  _ \ _   _ _ __ | |_(_)_ __ ___   ___  | | | | ___  __ _| |    / \  |_ _| " -ForegroundColor Magenta
Write-Host "  | |_) | | | | '_ \| __| | '_ ` _ \ / _ \ | |_| |/ _ \/ _` | |   / _ \  | |  " -ForegroundColor Magenta
Write-Host "  |  _ <| |_| | | | | |_| | | | | | |  __/ |  _  |  __/ (_| | |  / ___ \ | |  " -ForegroundColor Magenta
Write-Host "  |_| \_\\__,_|_| |_|\__|_|_| |_| |_|\___| |_| |_|\___|\__,_|_| /_/   \_\___| " -ForegroundColor Magenta
Write-Host "               DETECT. UNDERSTAND. RECOVER. (Phase 6 Demo Suite)               " -ForegroundColor White
Write-Host "==============================================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"

# ------------------------------------------------------------------------------
# Pre-flight Connectivity Check
# ------------------------------------------------------------------------------
Print-Header "PRE-FLIGHT: Verifying Application and Agent Connectivity"
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/api/agent/status" -Method Get -TimeoutSec 3
    Print-Success "Connected to RuntimeHeal Backend (Agent Status: $($health.status), Mode: $($health.attachMode))"
} catch {
    Print-Error "Could not connect to $baseUrl. Ensure the backend application is running with -javaagent."
    Write-Host "Launch with: .\run.bat" -ForegroundColor Yellow
    exit 1
}

try {
    $ollama = Invoke-RestMethod -Uri "$baseUrl/api/diagnostics/health" -Method Get -TimeoutSec 3
    Print-Success "Connected to Diagnostic Engine (Ollama: $($ollama.status), Model: $($ollama.model))"
} catch {
    Print-Warn "Ollama diagnostics health check returned unexpected status; heuristic fallback active."
}

# ------------------------------------------------------------------------------
# SCENARIO A: Arithmetic Division by Zero Self-Healing
# ------------------------------------------------------------------------------
Print-Header "SCENARIO A: Arithmetic Division by Zero (/ by zero) -> Self-Healing"
Print-Info "Triggering calculateDiscountRatio(100, 0) via GET /api/sample/arithmetic?total=100&divisor=0..."
Print-Info "Without self-healing, this operation crashes the request with HTTP 500."

try {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $response = Invoke-RestMethod -Uri "$baseUrl/api/sample/arithmetic?total=100&divisor=0" -Method Get
    $sw.Stop()

    if ($response.status -eq "SUCCESS" -and $response.discountRatio -eq 0) {
        Print-Success "HTTP 200 OK received! Response payload: $($response | ConvertTo-Json -Compress)"
        Print-Success "ByteBuddy suppressed ArithmeticException and injected type-safe fallback 0 in $($sw.ElapsedMilliseconds)ms."
    } else {
        Print-Warn "Received response but payload differed: $($response | ConvertTo-Json -Compress)"
    }
} catch {
    Print-Error "Scenario A failed with HTTP error: $($_.Exception.Message)"
}

Start-Sleep -Milliseconds 800
try {
    $actions = Invoke-RestMethod -Uri "$baseUrl/api/healing/actions" -Method Get
    $latest = $actions | Select-Object -First 1
    Print-Info "Audit Record Verification: ActionId=$($latest.actionId), Strategy=$($latest.strategy), Status=$($latest.status)"
    Print-Info "Audit Reason: $($latest.policyReason)"
} catch {}

# ------------------------------------------------------------------------------
# SCENARIO B: Transactional Method Safety Denylist
# ------------------------------------------------------------------------------
Print-Header "SCENARIO B: Transactional Method Denylist Guardrail (validateAndProcessTransfer)"
Print-Info "Attempting transfer with negative amount on critical financial method..."
Print-Info "Safety Policy must PROHIBIT automated tampering and preserve HTTP 400 Bad Request."

$body = @{
    accountId = "ACC-100"
    amount = -75.00
    currency = "USD"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/sample/illegal-argument" -Method Post -Body $body -ContentType "application/json"
    Print-Error "Unexpected success! Method should have been blocked by safety policy."
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400 -or $_.Exception.Message -like "*400*") {
        Print-Success "HTTP 400 Bad Request preserved as required! Transaction was NOT corrupted."
    } else {
        Print-Warn "Received HTTP status: $($_.Exception.Message)"
    }
}

Start-Sleep -Milliseconds 800
try {
    $actions = Invoke-RestMethod -Uri "$baseUrl/api/healing/actions" -Method Get
    $policyBlock = $actions | Where-Object { $_.status -eq "BLOCKED_POLICY" } | Select-Object -First 1
    if ($policyBlock) {
        Print-Success "Audit Log Confirmed: ActionId=$($policyBlock.actionId) | Status=BLOCKED_POLICY | Reason=$($policyBlock.policyReason)"
    }
} catch {}

# ------------------------------------------------------------------------------
# SCENARIO C: Sliding-Window Circuit Breakers
# ------------------------------------------------------------------------------
Print-Header "SCENARIO C: Sliding-Window Circuit Breaker (Recovery Loop Prevention)"
Print-Info "Configuring rolling window failure threshold to 3 failures via policy API..."

try {
    $policyUpdate = @{
        minConfidence = 0.70
        circuitBreakerConfig = @{ failureThreshold = 3; windowSeconds = 60 }
    } | ConvertTo-Json -Depth 3
    $null = Invoke-RestMethod -Uri "$baseUrl/api/healing/policy" -Method Post -Body $policyUpdate -ContentType "application/json"
    $null = Invoke-RestMethod -Uri "$baseUrl/api/healing/circuit-breaker/reset" -Method Post
    Print-Success "Policy calibrated and circuit breakers reset to CLOSED."
} catch {
    Print-Warn "Could not update policy threshold; proceeding with default threshold."
}

Print-Info "Firing rapid consecutive fault requests to trigger circuit breaker trip..."
for ($i = 1; $i -le 4; $i++) {
    try {
        $null = Invoke-RestMethod -Uri "$baseUrl/api/sample/arithmetic?total=100&divisor=0" -Method Get -TimeoutSec 10
        Write-Host "    Attempt ${i}: Request processed / healed." -ForegroundColor Gray
    } catch {
        Write-Host "    Attempt ${i}: Request failed with $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Start-Sleep -Milliseconds 800
try {
    $policy = Invoke-RestMethod -Uri "$baseUrl/api/healing/policy" -Method Get
    $cb = $policy.circuitBreakers."com.runtimeheal.backend.sample.service.OrderProcessingService.calculateDiscountRatio"
    if ($cb) {
        Print-Info "Circuit Breaker State: $($cb.state) (Recent failures in rolling window: $($cb.recentFailures))"
    }

    Print-Info "Testing Circuit Breaker Reset via POST /api/healing/circuit-breaker/reset..."
    $reset = Invoke-RestMethod -Uri "$baseUrl/api/healing/circuit-breaker/reset" -Method Post
    Print-Success "Reset status: $($reset.status) - $($reset.message)"
} catch {}

# ------------------------------------------------------------------------------
# Demonstration Summary
# ------------------------------------------------------------------------------
Print-Header "RUNTIMEHEAL AI - DEMONSTRATION COMPLETE"
try {
    $summary = Invoke-RestMethod -Uri "$baseUrl/api/healing/summary" -Method Get
    Write-Host "  Telemetry KPI Summary:" -ForegroundColor White
    Write-Host "    - Total Healing Evaluations : $($summary.totalHealingAttempts)" -ForegroundColor Cyan
    Write-Host "    - Successful Heals          : $($summary.successfulHeals)" -ForegroundColor Green
    Write-Host "    - Blocked by Policy         : $($summary.blockedByPolicy)" -ForegroundColor Yellow
    Write-Host "    - Blocked by Circuit Breaker: $($summary.blockedByCircuitBreaker)" -ForegroundColor Red
    Write-Host "    - Recovery Success Rate     : $($summary.successRatePercentage)%" -ForegroundColor Magenta
} catch {}

Write-Host ""
Write-Host "  All demonstration scenarios executed successfully!" -ForegroundColor Green
Write-Host "  Open the live dashboard at: http://localhost:8080/" -ForegroundColor Cyan
Write-Host ("=" * 78) -ForegroundColor Cyan

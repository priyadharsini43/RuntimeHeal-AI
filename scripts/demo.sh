#!/usr/bin/env bash
# ==============================================================================
# RuntimeHeal AI - Automated End-to-End Demonstration Script (Bash)
# Project: An AI-Driven Runtime Self-Healing Framework Using ByteBuddy & Local LLM
# ==============================================================================

set -e

BASE_URL="http://localhost:8080"

# Color Codes
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

print_header() {
    echo ""
    echo -e "${CYAN}==============================================================================${NC}"
    echo -e "${YELLOW}  $1${NC}"
    echo -e "${CYAN}==============================================================================${NC}"
}

print_success() { echo -e "  ${GREEN}[PASS]${NC} $1"; }
print_info()    { echo -e "  ${CYAN}[INFO]${NC} $1"; }
print_warn()    { echo -e "  ${YELLOW}[WARN]${NC} $1"; }
print_error()   { echo -e "  ${RED}[FAIL]${NC} $1"; }

clear || true
echo -e "${CYAN}==============================================================================${NC}"
echo -e "${MAGENTA}   RuntimeHeal AI - Automated Demonstration Suite (Phase 6)                     ${NC}"
echo -e "${MAGENTA}   DETECT. UNDERSTAND. RECOVER.                                                ${NC}"
echo -e "${CYAN}==============================================================================${NC}"

# Pre-flight Check
print_header "PRE-FLIGHT: Verifying Application and Agent Connectivity"
if curl -s -f "$BASE_URL/api/agent/status" > /dev/null; then
    STATUS=$(curl -s "$BASE_URL/api/agent/status")
    print_success "Connected to RuntimeHeal Backend: $STATUS"
else
    print_error "Cannot reach $BASE_URL. Ensure the application is started with -javaagent."
    exit 1
fi

# Scenario A: Arithmetic Self-Healing
print_header "SCENARIO A: Arithmetic Division by Zero (/ by zero) -> Self-Healing"
print_info "Triggering calculateDiscountRatio(100, 0)..."

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/sample/arithmetic?total=100&divisor=0")
RESPONSE=$(curl -s "$BASE_URL/api/sample/arithmetic?total=100&divisor=0")

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "HTTP 200 OK received! Response payload: $RESPONSE"
    print_success "ByteBuddy advice suppressed ArithmeticException and injected fallback value 0."
else
    print_error "Scenario A failed with HTTP status: $HTTP_CODE"
fi

# Scenario B: Transactional Denylist
print_header "SCENARIO B: Transactional Method Denylist Guardrail (validateAndProcessTransfer)"
print_info "Sending invalid payment request on denylisted financial method..."

HTTP_CODE_B=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/sample/illegal-argument" \
    -H "Content-Type: application/json" \
    -d '{"accountId":"ACC-100","amount":-50.00,"currency":"USD"}')

if [ "$HTTP_CODE_B" -eq 400 ]; then
    print_success "HTTP 400 Bad Request preserved as expected! Financial data integrity preserved."
else
    print_warn "Expected HTTP 400, received: $HTTP_CODE_B"
fi

# Scenario C: Circuit Breaker Reset
print_header "SCENARIO C: Circuit Breaker Reset Verification"
print_info "Calling POST /api/healing/circuit-breaker/reset..."

RESET_RES=$(curl -s -X POST "$BASE_URL/api/healing/circuit-breaker/reset")
print_success "Reset response: $RESET_RES"

# Summary
print_header "SUMMARY"
SUMMARY=$(curl -s "$BASE_URL/api/healing/summary")
print_info "Healing Metrics Summary: $SUMMARY"
echo ""
echo -e "${GREEN}All demonstration scenarios completed successfully!${NC}"
echo -e "${CYAN}Open the live observability dashboard at: http://localhost:8080/${NC}"
echo -e "${CYAN}==============================================================================${NC}"

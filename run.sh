#!/usr/bin/env bash
# ==============================================================================
# RuntimeHeal AI - System Startup Launcher (Bash)
# ==============================================================================

set -e

echo "=============================================================================="
echo "  RuntimeHeal AI - System Startup Launcher"
echo "  Detect. Understand. Recover."
echo "=============================================================================="

# Check Java
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed or not on PATH. JDK 17+ is required."
    exit 1
fi

# Build Agent if missing
if [ ! -f "agent/target/runtimeheal-agent-0.0.1-SNAPSHOT.jar" ]; then
    echo "[INFO] Agent JAR not found. Building agent..."
    (cd agent && mvn clean install -DskipTests)
fi

# Build Backend if missing
if [ ! -f "backend/target/runtimeheal-backend-0.0.1-SNAPSHOT.jar" ]; then
    echo "[INFO] Backend JAR not found. Packaging backend..."
    (cd backend && mvn package -DskipTests)
fi

echo ""
echo "[INFO] Starting RuntimeHeal AI with ByteBuddy Java Agent..."
echo "[INFO] Web Dashboard: http://localhost:8080/"
echo "[INFO] Press Ctrl+C to terminate."
echo ""

java -javaagent:agent/target/runtimeheal-agent-0.0.1-SNAPSHOT.jar -jar backend/target/runtimeheal-backend-0.0.1-SNAPSHOT.jar

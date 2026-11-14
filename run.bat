@echo off
setlocal

echo ==============================================================================
echo   RuntimeHeal AI - System Startup Launcher
echo   Detect. Understand. Recover.
echo ==============================================================================
echo.

:: Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed or not found on PATH. JDK 17+ is required.
    pause
    exit /b 1
)

:: Check if Agent JAR exists
if not exist "agent\target\runtimeheal-agent-0.0.1-SNAPSHOT.jar" (
    echo [INFO] Agent JAR not found. Building RuntimeHeal Agent...
    cd agent && call mvn clean install -DskipTests && cd ..
)

:: Check if Backend JAR exists
if not exist "backend\target\runtimeheal-backend-0.0.1-SNAPSHOT.jar" (
    echo [INFO] Backend JAR not found. Packaging RuntimeHeal Backend...
    cd backend && call mvn package -DskipTests && cd ..
)

echo.
echo [INFO] Starting RuntimeHeal AI with ByteBuddy Java Agent...
echo [INFO] Web Dashboard available at: http://localhost:8080/
echo [INFO] Press Ctrl+C to terminate application.
echo.

java -javaagent:agent\target\runtimeheal-agent-0.0.1-SNAPSHOT.jar -jar backend\target\runtimeheal-backend-0.0.1-SNAPSHOT.jar

endlocal

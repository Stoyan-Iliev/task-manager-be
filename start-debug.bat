@echo off
REM Script to start the application in debug mode
REM This script starts both postgres and the app with debugging enabled

echo.
echo 🐛 Starting Task Manager in debug mode...
echo.

REM Stop any running containers
echo 📦 Stopping existing containers...
docker compose down

REM Start with debug configuration
echo 🚀 Starting containers with debug configuration...
docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d

REM Wait for containers to start
echo.
echo ⏳ Waiting for containers to be healthy...
timeout /t 5 /nobreak > nul

REM Check if containers are running
docker ps | findstr taskmanager-app > nul
if %errorlevel% equ 0 (
    echo.
    echo ✅ Application started successfully!
    echo.
    echo 📋 Service Information:
    echo    Application: http://localhost:8080
    echo    Debug Port:  localhost:5005
    echo    PostgreSQL:  localhost:5432
    echo.
    echo 🔍 To attach debugger:
    echo    1. Open IntelliJ IDEA
    echo    2. Select 'Debug Docker Container' from the run configurations dropdown
    echo    3. Click the Debug button ^(or press Shift+F9^)
    echo.
    echo 📊 View logs:
    echo    docker compose logs -f app
    echo.
    echo 🛑 Stop containers:
    echo    docker compose down
) else (
    echo.
    echo ❌ Failed to start application. Check logs:
    echo    docker compose logs app
    exit /b 1
)

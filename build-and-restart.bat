@echo off
REM Build and Restart Script for Task Manager Backend (Windows)
REM This script compiles the application and restarts the Docker container

echo ============================================
echo Task Manager Backend - Build and Restart
echo ============================================
echo.

REM Step 1: Stop running containers
echo [INFO] Stopping running containers...
docker compose down --remove-orphans
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to stop containers
    exit /b 1
)
echo [SUCCESS] Containers stopped
echo.

REM Step 2: Clean and compile
echo [INFO] Cleaning and compiling project...
call mvn clean compile -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven compilation failed
    exit /b 1
)
echo [SUCCESS] Compilation successful
echo.

REM Step 3: Run tests (comment out to skip)
echo [INFO] Running tests...
call mvn test -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Tests failed
    exit /b 1
)
echo [SUCCESS] Tests passed
echo.

REM Step 4: Package application
echo [INFO] Packaging application...
call mvn package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Packaging failed
    exit /b 1
)
echo [SUCCESS] Application packaged
echo.

REM Step 5: Rebuild Docker image
echo [INFO] Rebuilding Docker image...
docker compose build --no-cache app
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker build failed
    exit /b 1
)
echo [SUCCESS] Docker image built
echo.

REM Step 6: Start containers
echo [INFO] Starting containers...
docker compose up -d
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to start containers
    exit /b 1
)
echo [SUCCESS] Containers started
echo.

REM Step 7: Wait and check status
echo [INFO] Waiting for application to be ready...
timeout /t 5 /nobreak >nul

docker compose ps
echo.
echo [SUCCESS] Application is running!
echo.
echo View logs with: docker compose logs -f app
echo Stop with: docker compose down
echo Health check: curl http://localhost:8080/actuator/health

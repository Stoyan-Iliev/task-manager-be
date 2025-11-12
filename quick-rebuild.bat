@echo off
REM Quick Rebuild Script (Cached, Skips Tests) - Windows
REM Use this for faster development iterations
REM Maven dependencies cached in %USERPROFILE%\.m2\repository
REM Docker layers cached for faster rebuilds

echo ==========================================
echo Quick Rebuild (Cached, Skips Tests)
echo ==========================================
echo.

echo [STEP] Stopping app container...
docker compose stop app
if errorlevel 1 (
    echo [ERROR] Failed to stop app
    exit /b 1
)
echo [SUCCESS] App stopped
echo.

echo [STEP] Packaging with Maven cache...
echo [INFO] Using cached dependencies from %USERPROFILE%\.m2\repository
call mvn package -DskipTests -q
if errorlevel 1 (
    echo [ERROR] Packaging failed
    exit /b 1
)
echo [SUCCESS] Package complete
echo.

echo [STEP] Rebuilding Docker image with cache...
echo [INFO] Using Docker layer cache for faster builds
docker compose build app
if errorlevel 1 (
    echo [ERROR] Docker build failed
    exit /b 1
)
echo [SUCCESS] Image rebuilt
echo.

echo [STEP] Starting app container...
docker compose up -d app
if errorlevel 1 (
    echo [ERROR] Failed to start app
    exit /b 1
)
echo [SUCCESS] App started
echo.

timeout /t 5 /nobreak >nul
echo [SUCCESS] Quick rebuild complete!
echo.
echo [STATUS]:
docker compose ps
echo.
echo [TIPS]:
echo   - Maven deps cached in %USERPROFILE%\.m2\repository
echo   - Docker layers reused when possible
echo   - Database kept running for faster restart
echo.
echo [COMMANDS]:
echo   Health check: curl http://localhost:8080/api/public/health
echo   View logs: docker compose logs -f app

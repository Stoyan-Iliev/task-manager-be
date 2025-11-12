# Build and Deployment Scripts

## Overview

This directory contains scripts to build and deploy the Task Manager backend application using Docker Compose. These scripts handle the common issue of the application already running and ensure clean rebuilds.

## Available Scripts

### 1. Full Build and Restart (With Tests)

**Linux/Mac:**
```bash
./build-and-restart.sh
```

**Windows (Command Prompt):**
```cmd
build-and-restart.bat
```

**What it does:**
1. ✅ Stops all running containers
2. ✅ Cleans previous builds
3. ✅ Compiles the source code
4. ✅ Runs all tests (unit + integration)
5. ✅ Packages the application (JAR)
6. ✅ Rebuilds Docker image (no cache)
7. ✅ Starts containers in detached mode
8. ✅ Verifies application is running

**Use when:**
- Making significant changes
- Before committing code
- Preparing for deployment
- Want to ensure all tests pass

**Duration:** ~2-3 minutes

---

### 2. Quick Rebuild (Skip Tests)

**Linux/Mac:**
```bash
./quick-rebuild.sh
```

**Windows (Command Prompt):**
```cmd
quick-rebuild.bat
```

**What it does:**
1. ⚡ Stops all running containers
2. ⚡ Cleans and packages (skips tests)
3. ⚡ Rebuilds Docker image (no cache)
4. ⚡ Starts containers
5. ⚡ Shows status

**Use when:**
- Rapid development iterations
- Testing UI/API changes quickly
- Making minor code changes
- Tests already passed recently

**Duration:** ~1 minute

---

## Usage Examples

### Scenario 1: Making Code Changes

```bash
# Edit your code
vim src/main/java/com/gradproject/taskmanager/...

# Quick rebuild to test
./quick-rebuild.sh

# Test your changes
curl http://localhost:8080/api/secure/organizations \
  -H "Authorization: Bearer YOUR_TOKEN"

# If working, do full build with tests
./build-and-restart.sh
```

### Scenario 2: Application Already Running Error

```bash
# If you get "port already in use" or "application already running"
# Just run the rebuild script - it handles cleanup automatically
./build-and-restart.sh
```

### Scenario 3: Database Migration Changes

```bash
# After creating new migration in db/migration/
./build-and-restart.sh

# Verify migration applied
docker compose exec postgres psql -U postgres -d taskmanager -c "\dt"
```

### Scenario 4: Docker Image Issues

```bash
# Force complete rebuild with no cache
docker compose down -v  # Remove volumes too
./build-and-restart.sh
```

---

## Script Output

### Successful Build
```
🔧 Task Manager Backend - Build and Restart
===========================================

ℹ Stopping running containers...
✓ Containers stopped

ℹ Cleaning and compiling project...
✓ Compilation successful

ℹ Running tests...
✓ Tests passed

ℹ Packaging application...
✓ Application packaged

ℹ Rebuilding Docker image...
✓ Docker image built

ℹ Starting containers...
✓ Containers started

ℹ Waiting for application to be ready...
✓ Application is running!

📊 Container Status:
NAME                  STATUS              PORTS
taskmanager-app       Up 5 seconds        0.0.0.0:8080->8080/tcp
taskmanager-postgres  Up 5 seconds        0.0.0.0:5432->5432/tcp

📝 View logs with: docker compose logs -f app
🛑 Stop with: docker compose down
🔍 Health check: curl http://localhost:8080/actuator/health
```

---

## Troubleshooting

### "Permission denied" on Linux/Mac
```bash
chmod +x build-and-restart.sh quick-rebuild.sh
```

### "mvn: command not found"
```bash
# Install Maven first
# Ubuntu/Debian:
sudo apt install maven

# macOS:
brew install maven

# Windows: Download from https://maven.apache.org/download.cgi
```

### "docker: command not found"
```bash
# Install Docker Desktop
# https://www.docker.com/products/docker-desktop
```

### Tests Failing
```bash
# Run tests separately to see detailed output
mvn test

# Or run specific test class
mvn test -Dtest=OrganizationServiceImplTest
```

### Container Won't Start
```bash
# Check logs
docker compose logs -f app

# Check if port is in use
# Linux/Mac:
lsof -i :8080

# Windows:
netstat -ano | findstr :8080

# Force cleanup and rebuild
docker compose down -v
docker system prune -f
./build-and-restart.sh
```

### Database Connection Issues
```bash
# Verify postgres is running
docker compose ps

# Check postgres logs
docker compose logs postgres

# Manually start just postgres
docker compose up -d postgres

# Wait a few seconds, then start app
docker compose up -d app
```

---

## Manual Commands (Alternative)

If you prefer running commands manually:

```bash
# Stop containers
docker compose down

# Build application
mvn clean package -DskipTests

# Rebuild Docker image
docker compose build app

# Start everything
docker compose up -d

# View logs
docker compose logs -f app
```

---

## Integration with Development Workflow

### Git Workflow
```bash
# Before committing
./build-and-restart.sh  # Ensure tests pass

# After pulling changes
./build-and-restart.sh  # Rebuild with new code

# During development
./quick-rebuild.sh      # Fast iterations
```

### IDE Integration

**IntelliJ IDEA:**
1. Right-click on `build-and-restart.sh`
2. "Run 'build-and-restart.sh'"
3. Or add as External Tool in Settings

**VS Code:**
1. Add to tasks.json:
```json
{
  "label": "Build and Restart",
  "type": "shell",
  "command": "./build-and-restart.sh",
  "problemMatcher": []
}
```

---

## Environment Variables

The scripts respect these environment variables (optional):

```bash
# Skip tests even in full build
SKIP_TESTS=true ./build-and-restart.sh

# Custom Maven arguments
MVN_ARGS="-P production" ./build-and-restart.sh

# Custom Docker Compose file
COMPOSE_FILE=docker-compose.prod.yml ./build-and-restart.sh
```

---

## Performance Tips

1. **Use Quick Rebuild for Development**
   - Tests take ~2 minutes, quick rebuild takes ~1 minute
   - Run full build before commits only

2. **Docker Layer Caching**
   - Scripts use `--no-cache` to ensure clean builds
   - For even faster builds, remove `--no-cache` flag (but may cause stale builds)

3. **Parallel Test Execution**
   ```bash
   # Edit pom.xml to enable parallel tests
   mvn test -T 4  # Use 4 threads
   ```

4. **Skip Integration Tests**
   ```bash
   mvn test -DskipITs
   ```

---

## Continuous Integration

These scripts can be used in CI/CD pipelines:

```yaml
# .github/workflows/build.yml
- name: Build and Test
  run: |
    chmod +x build-and-restart.sh
    ./build-and-restart.sh
```

---

## Related Files

- `docker-compose.yml` - Docker services configuration
- `Dockerfile` - Application container definition
- `pom.xml` - Maven build configuration
- `HIKARI-CONFIGURATION.md` - Database connection pool settings

---

## Support

If you encounter issues:

1. Check the logs: `docker compose logs -f`
2. Verify Docker is running: `docker ps`
3. Check disk space: `df -h`
4. Clean up Docker: `docker system prune -a`
5. Restart Docker Desktop

For more help, see the main `CLAUDE.md` file.

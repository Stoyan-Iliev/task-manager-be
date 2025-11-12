# Remote Debugging Setup for IntelliJ IDEA

This guide explains how to debug your Spring Boot application running in a Docker container using IntelliJ IDEA.

## Overview

The application can be run in debug mode with remote debugging enabled on port `5005`. IntelliJ IDEA can then attach to this port to provide full debugging capabilities including breakpoints, step-through debugging, variable inspection, and more.

## Quick Start

### Option 1: Using the Debug Scripts (Recommended)

**Linux/Mac:**
```bash
cd task-manager-be
./start-debug.sh
```

**Windows:**
```cmd
cd task-manager-be
start-debug.bat
```

This will:
1. Stop any running containers
2. Start PostgreSQL and the application with debugging enabled
3. Expose the debug port (5005) to your host machine

### Option 2: Using Docker Compose Directly

```bash
cd task-manager-be

# Stop any running containers
docker compose down

# Start with debug configuration
docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d

# View logs
docker compose logs -f app
```

## Attaching the Debugger in IntelliJ IDEA

Once the container is running in debug mode:

1. **Open IntelliJ IDEA** with your project

2. **Locate the Debug Configuration:**
   - Look for the run configuration dropdown in the toolbar (top-right)
   - Select **"Debug Docker Container"**

3. **Start Debugging:**
   - Click the Debug button (🐛) or press **Shift + F9**
   - You should see "Connected to the target VM, address: 'localhost:5005'" in the console

4. **Set Breakpoints:**
   - Click in the left gutter next to any line of code to set a breakpoint
   - When that line executes, the debugger will pause execution

5. **Test Your Setup:**
   - Set a breakpoint in a controller method (e.g., `AuthController.login()`)
   - Make an API request to that endpoint
   - The debugger should pause at your breakpoint

## Configuration Details

### Docker Debug Configuration

The debug setup uses two Docker Compose files:
- `docker-compose.yml` - Base configuration
- `docker-compose.debug.yml` - Debug-specific overrides

**Debug configuration (`docker-compose.debug.yml`):**
```yaml
services:
  app:
    environment:
      - JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
    ports:
      - "5005:5005"
```

**JVM Debug Parameters:**
- `transport=dt_socket` - Use socket transport for debugging
- `server=y` - JVM acts as debug server
- `suspend=n` - Application starts immediately (use `suspend=y` to wait for debugger)
- `address=*:5005` - Listen on all interfaces, port 5005

### IntelliJ Run Configuration

The debug configuration is stored in `.idea/runConfigurations/Debug_Docker_Container.xml`:

**Key settings:**
- **Type:** Remote JVM Debug
- **Host:** localhost
- **Port:** 5005
- **Debugger mode:** Attach to remote JVM

## Ports Reference

| Service | Port | Description |
|---------|------|-------------|
| Application | 8080 | Spring Boot HTTP server |
| Debug Port | 5005 | Remote JVM debugging |
| PostgreSQL | 5432 | Database |

## Troubleshooting

### Debugger Won't Connect

**Problem:** "Unable to open debugger port (localhost:5005)"

**Solutions:**
1. Verify the container is running:
   ```bash
   docker ps | grep taskmanager-app
   ```

2. Check if debug port is exposed:
   ```bash
   docker compose ps
   ```
   You should see `0.0.0.0:5005->5005/tcp` in the ports column

3. Check container logs for debug agent initialization:
   ```bash
   docker compose logs app | grep "Listening for transport"
   ```
   You should see: `Listening for transport dt_socket at address: 5005`

4. Verify no other process is using port 5005:
   ```bash
   # Linux/Mac
   lsof -i :5005

   # Windows
   netstat -ano | findstr :5005
   ```

### Breakpoints Not Working

**Problem:** Breakpoints show as gray/disabled or don't pause execution

**Solutions:**
1. Ensure you're running the exact same code:
   - Rebuild the Docker image after code changes:
     ```bash
     docker compose down
     docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d --build
     ```

2. Check that the debugger is actually attached:
   - IntelliJ console should show: "Connected to the target VM"

3. Verify source mapping:
   - IntelliJ needs to map your local source to the running code
   - Usually works automatically if project structure matches

### Application Starts Too Quickly

**Problem:** Want to debug startup code but application starts before debugger attaches

**Solution:** Use `suspend=y` in debug configuration:

1. Edit `docker-compose.debug.yml`:
   ```yaml
   - JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005
   ```

2. Restart containers:
   ```bash
   ./start-debug.sh
   ```

3. The application will wait for debugger before starting
4. Attach debugger in IntelliJ (Shift+F9)
5. Application will start after debugger connects

### Port Already in Use

**Problem:** "Bind for 0.0.0.0:5005 failed: port is already allocated"

**Solutions:**
1. Stop other Docker containers using that port:
   ```bash
   docker ps
   docker stop <container-id>
   ```

2. Change the debug port in both files:
   - `docker-compose.debug.yml`: Change `5005:5005` to `5006:5005`
   - `.idea/runConfigurations/Debug_Docker_Container.xml`: Change port to `5006`

## Advanced Usage

### Debugging with Hot Reload

For faster development, you can combine debugging with volume mounting to see code changes without rebuilding:

1. Add volume mount to `docker-compose.debug.yml`:
   ```yaml
   services:
     app:
       volumes:
         - ./target/classes:/app/classes
   ```

2. Use Spring Boot DevTools in your `pom.xml`

3. Your IDE will compile changes, and Spring will reload them

### Remote Debugging Production Issues

**⚠️ Warning:** Only use in secure, non-production environments!

To debug a remote server:

1. SSH tunnel to remote debug port:
   ```bash
   ssh -L 5005:localhost:5005 user@remote-server
   ```

2. Use the same IntelliJ configuration
3. Set breakpoints and debug as normal

## Best Practices

1. **Don't commit debug configurations to production:**
   - Keep `docker-compose.debug.yml` separate
   - Never expose debug ports in production

2. **Use conditional breakpoints:**
   - Right-click breakpoint → Add condition
   - Only pause when specific conditions are met
   - Example: `userId == 123`

3. **Use logpoint instead of breakpoints for production:**
   - Right-click breakpoint → "Breakpoint" → Uncheck "Suspend"
   - Check "Log" to log expressions instead

4. **Close debug sessions when done:**
   - Reduces resource usage
   - Prevents accidental breakpoints in shared environments

## Additional Resources

- [IntelliJ IDEA Remote Debug Documentation](https://www.jetbrains.com/help/idea/tutorial-remote-debug.html)
- [JDWP Protocol Specification](https://docs.oracle.com/javase/8/docs/technotes/guides/jpda/jdwp-spec.html)
- [Spring Boot Docker Debugging](https://spring.io/guides/topicals/spring-boot-docker/)

## Quick Reference Commands

```bash
# Start in debug mode
./start-debug.sh

# View application logs
docker compose logs -f app

# View debug port status
docker compose ps

# Restart containers
docker compose restart app

# Stop all containers
docker compose down

# Rebuild and restart
docker compose down
docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d --build
```

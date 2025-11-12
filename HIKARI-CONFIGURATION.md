# HikariCP Connection Pool Configuration

## Overview

HikariCP is configured across three profiles with optimized settings for each environment.

## Configuration Profiles

### 1. Default Profile (application.yml)
**Environment**: Local development

```yaml
spring.datasource.hikari:
  pool-name: TaskManagerHikariPool
  maximum-pool-size: 10          # Max connections in pool
  minimum-idle: 5                # Min idle connections maintained
  connection-timeout: 30000      # 30 seconds - max wait time for connection
  idle-timeout: 600000           # 10 minutes - max time connection can sit idle
  max-lifetime: 1800000          # 30 minutes - max lifetime of connection
  auto-commit: true              # Auto-commit transactions
  connection-test-query: SELECT 1 # Validation query
  leak-detection-threshold: 60000 # 60 seconds - detect connection leaks
```

### 2. Docker Profile (application-docker.yml)
**Environment**: Docker containerized deployment

```yaml
spring.datasource.hikari:
  pool-name: TaskManagerDockerHikariPool
  maximum-pool-size: 15          # Higher for production load
  minimum-idle: 5
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
  auto-commit: true
  connection-test-query: SELECT 1
  leak-detection-threshold: 60000
```

### 3. Test Profile (src/test/resources/application.yml)
**Environment**: Unit and integration tests

```yaml
spring.datasource.hikari:
  pool-name: TaskManagerTestHikariPool
  maximum-pool-size: 5           # Smaller for test efficiency
  minimum-idle: 2                # Minimal idle connections
  connection-timeout: 10000      # 10 seconds - faster timeout
  idle-timeout: 300000           # 5 minutes
  max-lifetime: 600000           # 10 minutes - shorter for tests
  auto-commit: true
```

## Configuration Details

### Pool Sizing
- **maximum-pool-size**: Maximum number of connections that can be created
  - **Local**: 10 (sufficient for single developer)
  - **Docker**: 15 (handles moderate production load)
  - **Test**: 5 (minimal for test execution)

- **minimum-idle**: Minimum idle connections HikariCP tries to maintain
  - Prevents connection creation overhead during spikes
  - If idle connections < minimum-idle, HikariCP creates new ones up to maximum-pool-size

### Timeouts
- **connection-timeout**: Max time (ms) to wait for connection from pool
  - If exceeded, throws SQLException
  - 30 seconds is reasonable for production, 10 seconds for tests

- **idle-timeout**: Max time (ms) connection can sit idle before being retired
  - Only applies if current connections > minimum-idle
  - 10 minutes balances resource usage and connection overhead

- **max-lifetime**: Max lifetime (ms) of connection in the pool
  - Protects against connection leaks at database/network level
  - 30 minutes is recommended (less than database timeout)

### Connection Management
- **auto-commit**: Whether connections have auto-commit enabled by default
  - Set to `true` for standard JDBC behavior
  - Spring @Transactional overrides this when needed

- **connection-test-query**: Query to validate connection liveness
  - `SELECT 1` is lightweight and works with PostgreSQL
  - Used when connection is borrowed from pool (if validation enabled)

- **leak-detection-threshold**: Time (ms) before connection leak is logged
  - Helps identify connections not properly closed
  - 60 seconds is reasonable for most applications

## Verification

You can verify HikariCP configuration by:

1. **Checking application logs on startup**:
   ```bash
   mvn spring-boot:run | grep -i hikari
   ```

2. **Running tests with DEBUG logging**:
   ```bash
   mvn test -Dlogging.level.com.zaxxer.hikari.HikariConfig=DEBUG
   ```

3. **Expected output**:
   ```
   HikariPool-1 - Starting...
   HikariPool-1 - Added connection
   HikariPool-1 - Start completed.
   ```

## Best Practices

1. **Pool Size Formula**:
   - Recommended: `connections = ((core_count * 2) + effective_spindle_count)`
   - For most applications: 10-20 is optimal
   - Don't over-provision (more isn't always better)

2. **Connection Lifetime**:
   - Keep `max-lifetime` less than database connection timeout
   - PostgreSQL default is 10 hours, we use 30 minutes for safety

3. **Leak Detection**:
   - Enable in development/staging to catch bugs early
   - Can be disabled in production if confident

4. **Monitoring**:
   - Use Spring Actuator metrics to monitor pool usage
   - Available at: `/actuator/metrics/hikaricp.*`

## Troubleshooting

### "Connection is not available"
- Increase `maximum-pool-size` or `connection-timeout`
- Check for connection leaks (not closing connections)

### "Connection has been closed"
- Check `max-lifetime` setting
- Verify database timeout settings

### Slow performance
- Ensure `minimum-idle` is set appropriately
- Monitor `connection-timeout` occurrences
- Check database query performance

## References

- [HikariCP GitHub](https://github.com/brettwooldridge/HikariCP)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [Spring Boot HikariCP](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#appendix.application-properties.data)

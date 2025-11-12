# Task Manager Backend - Quick Start Guide

## 🚀 Getting Started

### Prerequisites
- Java 21
- Maven 3.8+
- Docker Desktop
- PostgreSQL (via Docker)

### Initial Setup

1. **Clone and navigate:**
   ```bash
   cd task-manager-be
   ```

2. **First time build:**
   ```bash
   ./build-and-restart.sh    # Linux/Mac
   build-and-restart.bat     # Windows
   ```

3. **Verify it's running:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

---

## ⚡ Development Workflow

### Making Code Changes

```bash
# 1. Edit your code
vim src/main/java/...

# 2. Quick rebuild (skip tests)
./quick-rebuild.sh         # Takes ~1 minute

# 3. Test your changes
curl http://localhost:8080/api/...

# 4. Before commit: Full build with tests
./build-and-restart.sh     # Takes ~2-3 minutes
```

### Common Commands

| Task                          | Command                                                |
|-------------------------------|--------------------------------------------------------|
| **Quick rebuild** (no tests)  | `./quick-rebuild.sh`                                   |
| **Full rebuild** (with tests) | `./build-and-restart.sh`                               |
| **View logs**                 | `docker compose logs -f app`                           |
| **Stop services**             | `docker compose down`                                  |
| **Run tests only**            | `mvn test`                                             |
| **Check coverage**            | `mvn verify` then open `target/site/jacoco/index.html` |

---

## 📊 Available Endpoints

### Public Endpoints
- `GET /api/public/health` - Health check
- `POST /api/auth/signup` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh access token

### Protected Endpoints (require JWT)
- `GET /api/secure/organizations` - List my organizations
- `POST /api/secure/organizations` - Create organization
- `GET /api/secure/organizations/{id}` - Get organization
- `PUT /api/secure/organizations/{id}` - Update organization
- `DELETE /api/secure/organizations/{id}` - Delete organization
- `POST /api/secure/organizations/{orgId}/members` - Add member
- `GET /api/secure/organizations/{orgId}/members` - List members

### Actuator Endpoints
- `GET /actuator/health` - Application health
- `GET /actuator/info` - Application info

---

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=OrganizationServiceImplTest
```

### Run Integration Tests
```bash
mvn -Ptest-it verify
```

### Coverage Report
```bash
mvn verify
open target/site/jacoco/index.html
```

---

## 🗄️ Database

### Access Database (Docker)
```bash
docker compose exec postgres psql -U postgres -d taskmanager
```

### Common SQL Commands
```sql
-- List tables
\dt

-- Describe table
\d organizations

-- View migrations
SELECT * FROM flyway_schema_history;

-- Count organizations
SELECT COUNT(*) FROM organizations;
```

### Reset Database
```bash
docker compose down -v
./build-and-restart.sh
```

---

## 🐛 Troubleshooting

### "Port 8080 already in use"
```bash
./build-and-restart.sh  # Handles cleanup automatically
```

### "Cannot connect to database"
```bash
# Start postgres separately
docker compose up -d postgres
sleep 5
docker compose up -d app
```

### "Tests failing"
```bash
# Run tests with detailed output
mvn test

# Skip tests for quick rebuild
./quick-rebuild.sh
```

### "Docker image won't build"
```bash
# Force clean rebuild
docker compose down -v
docker system prune -f
./build-and-restart.sh
```

### "Application won't start"
```bash
# Check logs
docker compose logs -f app

# Check if postgres is ready
docker compose ps
```

---

## 📝 Configuration Files

| File                                 | Purpose                           |
|--------------------------------------|-----------------------------------|
| `application.yml`                    | Default config (local PostgreSQL) |
| `application-docker.yml`             | Docker environment config         |
| `src/test/resources/application.yml` | Test config (H2)                  |
| `docker-compose.yml`                 | Docker services                   |
| `HIKARI-CONFIGURATION.md`            | Connection pool settings          |

---

## 🔐 Getting a JWT Token

### 1. Register
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### 3. Use Token
```bash
TOKEN="your_access_token_here"

curl http://localhost:8080/api/secure/organizations \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📚 Additional Documentation

- `BUILD-SCRIPTS.md` - Detailed build script documentation
- `HIKARI-CONFIGURATION.md` - Database connection pool configuration
- `CLAUDE.md` - Complete project documentation
- `12-DAY-IMPLEMENTATION.md` - Implementation roadmap
- `STRATEGIC-ROADMAP.md` - Long-term feature planning

---

## 🔥 Quick Reference

### Daily Development Loop
```bash
# Morning: Pull latest changes
git pull
./build-and-restart.sh

# Development: Make changes
vim src/...
./quick-rebuild.sh

# Before commit: Full build
./build-and-restart.sh
git add .
git commit -m "feat: add new feature"
```

### Performance Monitoring
```bash
# View HikariCP metrics
curl http://localhost:8080/actuator/metrics/hikaricp.connections

# View JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

---

## 💡 Tips

1. **Use quick-rebuild.sh during development** - much faster
2. **Run full build before commits** - ensures all tests pass
3. **Check logs if something breaks** - `docker compose logs -f app`
4. **Database changes require full rebuild** - migrations need clean state
5. **Keep Docker Desktop running** - required for PostgreSQL

---

## 🆘 Getting Help

1. Check logs: `docker compose logs -f`
2. Read error messages carefully
3. See `BUILD-SCRIPTS.md` for detailed troubleshooting
4. See `CLAUDE.md` for architecture guidance
5. Check GitHub issues

---

**Ready to start?** Run `./build-and-restart.sh` and start coding! 🚀

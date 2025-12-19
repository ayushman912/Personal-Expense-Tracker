# Personal Expense Tracker

> A full-stack Java expense tracking application with JavaFX GUI, Jetty-based REST API, and multi-user support.

![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk)
![Maven](https://img.shields.io/badge/Maven-3.6+-C71A36?logo=apache-maven)
![License](https://img.shields.io/badge/License-Educational-green)
![Tests](https://img.shields.io/badge/Tests-JUnit%205-success)
![Coverage](https://img.shields.io/badge/Coverage-Unit%20%26%20Integration-yellow)

---

## Table of Contents

- [Quick Demo / TL;DR](#quick-demo--tldr)
- [Features](#features)
- [Architecture & Project Layout](#architecture--project-layout)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Database Setup](#database-setup)
- [Build & Run](#build--run)
- [API Reference](#api-reference)
- [Sample curl Commands](#sample-curl-commands)
- [Tests & CI](#tests--ci)
- [Security & Privacy Notes](#security--privacy-notes)
- [Backup & Recovery](#backup--recovery)
- [Troubleshooting](#troubleshooting)
- [Evidence Checklist for Examiners](#evidence-checklist-for-examiners)
- [Changelog](#changelog)
- [Contribution & License](#contribution--license)
- [Contact & Credits](#contact--credits)
- [Appendix](#appendix)

---

## Quick Demo / TL;DR

**Personal Expense Tracker** is a multi-user financial management application featuring a JavaFX desktop client and a RESTful Jetty server. Users can track expenses and income, view reports with charts, import/export CSV files, and backup data—all with secure token-based authentication and BCrypt password hashing.

### Run in 4 Commands

```bash
git clone <repo-url> && cd PET2
./mvnw clean package -DskipTests
java -jar server/target/server-1.0.0.jar          # Start server on port 8080
cd client && ../mvnw javafx:run                   # Start JavaFX client
```

> **Default credentials:** `admin` / `admin123`

---

## Features

- **Multi-user Expense/Income Tracking** — Each user's data is strictly isolated
- **JavaFX GUI Client** — Modern desktop interface with charts and filters
- **Jetty Servlet Backend** — Embedded server, no external container required
- **Token-based Authentication (Bearer)** — Secure API access with UUID tokens
- **BCrypt Password Hashing** — Industry-standard password security (cost factor 12)
- **Token TTL & Automatic Expiration** — 30-minute sliding expiration with background cleanup
- **AuthFilter Security** — Enforces authentication on all `/api/*` endpoints (except `/api/auth/*`)
- **HikariCP Connection Pooling** — High-performance database connections (10 max pool size)
- **Offline/Online Sync** — `SyncManager` queues operations when offline; syncs when connectivity resumes
- **CSV Import/Export** — Bulk data operations with progress tracking
- **Database Backup** — Supports both MySQL (JDBC-based SQL dump) and H2 (`SCRIPT TO` command)
- **Unit & Integration Tests** — JUnit 5 tests covering DAO, services, and security
- **Category Management** — System-wide default categories + user-specific custom categories

---

## Architecture & Project Layout

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              JavaFX Client                                  │
│  ┌───────────────┐  ┌───────────────┐  ┌─────────────────────────────────┐  │
│  │ UI Controllers│  │ SyncManager   │  │ ConnectivityManager             │  │
│  └───────────────┘  └───────────────┘  └─────────────────────────────────┘  │
│           │                  │                         │                    │
│           └──────────────────┼─────────────────────────┘                    │
│                              ▼                                              │
│                    ┌─────────────────┐                                      │
│                    │ RemoteRepository│ ◄── HTTP Client (java.net.http)      │
│                    └─────────────────┘                                      │
└──────────────────────────────┼──────────────────────────────────────────────┘
                               │ REST API (JSON)
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Jetty Server (Port 8080)                          │
│  ┌───────────────┐  ┌───────────────────────────────────────────────────┐   │
│  │  AuthFilter   │──│ Servlets: Auth, Register, Transaction, Category,  │   │  │  (Security)   │  │           Backup                                  │   │
│  └───────────────┘  └───────────────────────────────────────────────────┘   │
│           │                              │                                  │
│           ▼                              ▼                                  │
│  ┌───────────────┐              ┌─────────────────┐                         │
│  │  TokenStore   │              │   DAO Layer     │                         │
│  │  (Sessions)   │              │ (User, Trans,   │                         │
│  └───────────────┘              │  Category)      │                         │
│                                 └─────────────────┘                         │
│                                          │                                  │
│                                          ▼                                  │
│                                 ┌─────────────────┐                         │
│                                 │ DatabaseManager │ ◄── HikariCP Pool       │
│                                 └─────────────────┘                         │
└──────────────────────────────────────────┼──────────────────────────────────┘
                                           │ JDBC
                                           ▼
                              ┌─────────────────────────┐
                              │   MySQL 8.0+ / H2 DB    │
                              └─────────────────────────┘
```

### Module Structure

```
PET2/
├── pom.xml                          # Parent POM (multi-module)
├── mvnw, mvnw.cmd                   # Maven wrapper
├── README.md                        # This file
├── docs/
│   ├── api.md                       # API documentation
|   └── PROJECT_DOCUMENTATION.md     # Project documentation
│
├── shared/                          # Shared module (models, utils)
│   ├── pom.xml
│   └── src/main/java/com/expensetracker/
│       ├── model/                   # Transaction, Category, User models
│       ├── dao/                     # DAO interfaces
│       ├── dao/impl/                # DAO implementations (UserDAOImpl)
│       ├── util/                    # DatabaseManager, PasswordUtil, JsonUtil, Paginator
│       └── exceptions/              # Custom exceptions
│
├── server/                          # Server module (REST API)
│   ├── pom.xml
│   └── src/main/java/com/expensetracker/server/
│       ├── ServerApp.java           # Jetty server bootstrap
│       ├── filter/AuthFilter.java   # Security filter
│       ├── util/TokenStore.java     # Token management with TTL
│       └── servlet/                 # REST endpoints
│           ├── AuthServlet.java     # POST /api/auth/login
│           ├── RegisterServlet.java # POST /api/auth/register
│           ├── TransactionServlet.java
│           ├── CategoryServlet.java
│           └── BackupServlet.java
│
└── client/                          # Client module (JavaFX GUI)
    ├── pom.xml
    └── src/main/java/com/expensetracker/
        ├── MainApp.java             # JavaFX entry point
        ├── ui/                      # FXML controllers
        ├── client/api/              # RemoteRepository (HTTP client)
        └── client/service/          # SyncManager, ConnectivityManager
```

### Authentication Flow

```
1. Client → POST /api/auth/login { username, password }
2. Server → UserDAO.authenticate() → BCrypt.checkpw()
3. Server → TokenStore.addToken(uuid, username, userId)
4. Server → 200 { token, user, userId }
5. Client → Stores token, sets Authorization: Bearer <token>
6. Client → GET /api/transactions (with header)
7. Server → AuthFilter validates token via TokenStore.isValid()
8. Server → Sets userId in request attributes for data isolation
9. Server → TransactionServlet.doGet() queries by userId only
```

### Offline Sync Flow

```
1. ConnectivityManager detects server unreachable (every 5s poll)
2. User performs CRUD → SyncManager.queueOperation() → sync_queue table
3. ConnectivityManager detects server online
4. SyncManager.sync() → processes queued operations via RemoteRepository
5. On success → delete from sync_queue; on failure → increment retry_count
6. After max 3 retries → operation removed from queue
```

---

## Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| **Java (JDK)** | 21+ | Required for JavaFX 21 and records |
| **Maven** | 3.6+ | Or use included `mvnw` wrapper |
| **MySQL** | 8.0+ | Production database (optional) |
| **H2** | 2.2.x | Default for development/testing (bundled) |

### Optional Tools

- **mysqldump** — For manual MySQL backups
- **IDE** — IntelliJ IDEA, Eclipse, or VS Code with Java extensions

---

## Configuration

### Configuration File Location

The application reads configuration from `shared/src/main/resources/config.properties`.

### Sample `config.properties`

```properties
# =============================================================================
# Personal Expense Tracker - Configuration
# =============================================================================

# Database Configuration
# ----------------------
# For H2 (Development/Testing - Default):
db.url=jdbc:h2:./expense_tracker_db;DB_CLOSE_DELAY=-1;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE
db.user=sa
db.password=

# For MySQL (Production):
# db.url=jdbc:mysql://localhost:3306/expense_tracker?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
# db.user=${DB_USER}
# db.password=${DB_PASS}

# HikariCP Connection Pool (configured in DatabaseManager.java)
# hikari.maximumPoolSize=10
# hikari.minimumIdle=2
# hikari.connectionTimeout=30000
# hikari.idleTimeout=600000
# hikari.maxLifetime=1800000

# Token Configuration (configured in TokenStore.java)
# token.ttl.minutes=30
# token.cleanup.interval.minutes=5

# Backup Configuration
# backup.dir=backups
```

### Environment Variables (Production)

For production deployments, use environment variables to avoid hardcoded credentials:

```bash
# Linux/macOS
export DB_USER=expense_user
export DB_PASS=your_secure_password

# Windows PowerShell
$env:DB_USER="expense_user"
$env:DB_PASS="your_secure_password"
```

Then reference in `config.properties`:
```properties
db.user=${DB_USER}
db.password=${DB_PASS}
```

> **⚠️ Security Note:** Never commit credentials to version control. Use `.env` files (add to `.gitignore`) or CI/CD secrets.

---

## Database Setup

### Option 1: H2 Database (Default - Zero Configuration)

H2 runs embedded with the application. No setup required!

The database file is created automatically at `./expense_tracker_db.mv.db`.

**To reset H2 database:**
```bash
rm -f expense_tracker_db.mv.db expense_tracker_db.trace.db
```

### Option 2: MySQL Database (Production)

#### Step 1: Start MySQL Server

```bash
# Windows
net start MySQL80

# Linux/macOS
sudo systemctl start mysql
```

#### Step 2: Create Database and User

```sql
-- Connect as root
mysql -u root -p

-- Create database
CREATE DATABASE expense_tracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create application user (replace 'your_password')
CREATE USER 'expense_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON expense_tracker.* TO 'expense_user'@'localhost';
FLUSH PRIVILEGES;
```

#### Step 3: Load Schema

```bash
mysql -u expense_user -p expense_tracker < client/src/main/resources/sql/schema.sql
```

#### Step 4: Update Configuration

Edit `shared/src/main/resources/config.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/expense_tracker?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.user=expense_user
db.password=your_password
```

### Seed Sample Data (Optional)

```bash
mysql -u expense_user -p expense_tracker < client/src/main/resources/sql/sample_data.sql
```

> **Note:** Sample data uses plain-text passwords. The application will handle BCrypt hashing for new users automatically.

### Switching Between H2 and MySQL

Simply update `db.url` in `config.properties` and rebuild:

| Mode | `db.url` prefix |
|------|-----------------|
| H2 | `jdbc:h2:` |
| MySQL | `jdbc:mysql:` |

---

## Build & Run

### Build All Modules

```bash
./mvnw clean package
```

Expected output:
```
[INFO] Reactor Summary:
[INFO] Personal Expense Tracker Parent .................... SUCCESS
[INFO] shared ............................................. SUCCESS
[INFO] server ............................................. SUCCESS
[INFO] client ............................................. SUCCESS
[INFO] BUILD SUCCESS
```

### Run Server

```bash
# Using the shaded JAR (recommended)
java -jar server/target/server-1.0.0.jar

# Or from source
cd server && ../mvnw exec:java -Dexec.mainClass=com.expensetracker.server.ServerApp
```

Server starts on **port 8080** by default. You should see:
```
Initializing Database...
Starting server on port 8080...
```

### Run JavaFX Client

```bash
cd client
../mvnw javafx:run
```

### Changing Server Port

Modify `server/src/main/java/com/expensetracker/server/ServerApp.java` line 22:
```java
public static void main(String[] args) throws Exception {
    Server server = startServer(8080); // Change port here
    server.join();
}
```

Or pass as argument (requires code modification to read args).

### Health Check

```bash
curl http://localhost:8080/health
# Response: Server is running
```

---

## API Reference

### Base URL
`http://localhost:8080`

### Authentication Endpoints

#### Register New User

| Property | Value |
|----------|-------|
| **Endpoint** | `POST /api/auth/register` |
| **Auth Required** | No |
| **Content-Type** | `application/json` |

**Request Body:**
```json
{
  "username": "newuser",
  "email": "newuser@example.com",
  "password": "securePass123"
}
```

**Validation Rules:**
- `username`: Required, 3-30 characters, alphanumeric and underscores only
- `email`: Optional, validated if provided
- `password`: Required, minimum 6 characters

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| 201 | Created | `{"message": "User created successfully", "userId": 5}` |
| 400 | Validation error | `{"error": "Username must be 3-30 characters..."}` |
| 409 | Username exists | `{"error": "Username already exists"}` |
| 500 | Server error | `{"error": "Internal server error"}` |

---

#### Login

| Property | Value |
|----------|-------|
| **Endpoint** | `POST /api/auth/login` |
| **Auth Required** | No |
| **Content-Type** | `application/json` |

**Request Body:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Responses:**

| Status | Description | Body |
|--------|-------------|------|
| 200 | Success | `{"token": "uuid-token", "user": {...}, "userId": 1}` |
| 400 | Missing fields | `{"error": "Username and password required"}` |
| 401 | Invalid credentials | `{"error": "Invalid credentials"}` |

---

### Transaction Endpoints

All transaction endpoints require `Authorization: Bearer <token>` header.

#### Get All Transactions

| Property | Value |
|----------|-------|
| **Endpoint** | `GET /api/transactions` |
| **Auth Required** | Yes |

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `startDate` | `YYYY-MM-DD` | Filter start date (optional) |
| `endDate` | `YYYY-MM-DD` | Filter end date (optional) |
| `categoryId` | `int` | Filter by category (optional) |

**Response (200):**
```json
[
  {
    "id": 1,
    "userId": 1,
    "amount": 45.50,
    "description": "Grocery shopping",
    "date": "2024-01-15",
    "categoryId": 3,
    "type": "EXPENSE"
  }
]
```

---

#### Create Transaction

| Property | Value |
|----------|-------|
| **Endpoint** | `POST /api/transactions` |
| **Auth Required** | Yes |
| **Content-Type** | `application/json` |

**Request Body:**
```json
{
  "amount": 100.00,
  "description": "Groceries",
  "date": "2024-01-27",
  "categoryId": 3,
  "type": "EXPENSE"
}
```

**Responses:**

| Status | Description |
|--------|-------------|
| 201 | Created (returns transaction with ID) |
| 400 | Invalid data |
| 401 | Unauthorized |

---

#### Update Transaction

| Property | Value |
|----------|-------|
| **Endpoint** | `PUT /api/transactions` |
| **Auth Required** | Yes |
| **Content-Type** | `application/json` |

**Request Body:**
```json
{
  "id": 1,
  "amount": 150.00,
  "description": "Updated groceries",
  "date": "2024-01-27",
  "categoryId": 3,
  "type": "EXPENSE"
}
```

---

#### Delete Transaction

| Property | Value |
|----------|-------|
| **Endpoint** | `DELETE /api/transactions/{id}` |
| **Auth Required** | Yes |

**Response:** `204 No Content`

---

### Category Endpoints

#### Get All Categories

| Property | Value |
|----------|-------|
| **Endpoint** | `GET /api/categories` |
| **Auth Required** | Yes |

Returns system categories (user_id = NULL) plus user-specific categories.

---

#### Create Category

| Property | Value |
|----------|-------|
| **Endpoint** | `POST /api/categories` |
| **Auth Required** | Yes |
| **Content-Type** | `application/json` |

**Request Body:**
```json
{
  "name": "Subscriptions",
  "type": "EXPENSE",
  "description": "Monthly subscriptions"
}
```

---

### Backup Endpoint

#### Trigger Backup

| Property | Value |
|----------|-------|
| **Endpoint** | `POST /api/backup` |
| **Auth Required** | Yes |

**Response (200):**
```json
{
  "status": "success",
  "message": "Backup created successfully",
  "filename": "backup_20241217_143022.sql"
}
```

---

## Sample curl Commands

### Register a New User

```bash
curl -i -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"pass123"}'
```

### Login and Extract Token

```bash
# Login
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token'

# Store token in variable (bash)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')
```

### Access Protected Endpoint (Without Token → 401)

```bash
curl -i http://localhost:8080/api/transactions
# Response: 401 Unauthorized
# Body: {"error":"Unauthorized access"}
```

### Access Protected Endpoint (With Token → 200)

```bash
curl -i http://localhost:8080/api/transactions \
  -H "Authorization: Bearer $TOKEN"
# Response: 200 OK
# Body: [...]
```

### Create a Transaction

```bash
curl -i -X POST http://localhost:8080/api/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 75.50,
    "description": "Weekly groceries",
    "date": "2024-12-17",
    "categoryId": 3,
    "type": "EXPENSE"
  }'
```

### Get Transactions with Filters

```bash
curl -i "http://localhost:8080/api/transactions?startDate=2024-01-01&endDate=2024-12-31&categoryId=3" \
  -H "Authorization: Bearer $TOKEN"
```

### Trigger Database Backup

```bash
curl -i -X POST http://localhost:8080/api/backup \
  -H "Authorization: Bearer $TOKEN"
```

### Get Categories

```bash
curl -i http://localhost:8080/api/categories \
  -H "Authorization: Bearer $TOKEN"
```

---

## Tests & CI

### Run All Tests

```bash
./mvnw test
```

### Run Tests for Specific Module

```bash
# Server tests only
cd server && ../mvnw test

# Client tests only
cd client && ../mvnw test
```

### Test Coverage Summary

| Test Class | Module | Coverage |
|------------|--------|----------|
| `IsolationTest` | server | Multi-user data isolation |
| `SecurityIntegrationTest` | server | Auth filter, 401/200 responses |
| `RegistrationIntegrationTest` | server | User registration flow |
| `ServerIntegrationTest` | server | Full API integration |
| `TransactionDAOImplTest` | client | DAO CRUD operations |
| `PaginatorTest` | client | Generic pagination utility |
| `ReportGeneratorTest` | client | Report calculations |
| `CSVExporterTest` | client | CSV export functionality |
| `ClientIntegrationTest` | client | Client-server integration |

### GitHub Actions CI

Create `.github/workflows/ci.yml`:

```yaml
name: Java CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    
    - name: Build and Test
      run: ./mvnw -q clean test package
    
    - name: Upload Test Results
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: test-results
        path: '**/target/surefire-reports/*.xml'
```

---

## Security & Privacy Notes

### Password Hashing

- **Algorithm:** BCrypt with cost factor 12
- **Implementation:** `shared/src/main/java/com/expensetracker/util/PasswordUtil.java` (lines 13-30)
- **Usage:** Passwords hashed in `shared/src/main/java/com/expensetracker/dao/impl/UserDAOImpl.java` line 64

```java
// PasswordUtil.java (lines 13-15)
private static final int BCRYPT_ROUNDS = 12;
// ...
return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
```

### Token TTL Configuration

- **Default TTL:** 30 minutes (sliding expiration)
- **Cleanup Interval:** Every 5 minutes
- **Implementation:** `server/src/main/java/com/expensetracker/server/util/TokenStore.java` (lines 19-23)

```java
// TokenStore.java (lines 19-23)
private static final long TOKEN_TTL_MS = 30 * 60 * 1000; // 30 minutes
private static final long CLEANUP_INTERVAL_MINUTES = 5;
```

### No Hardcoded Credentials

- Database credentials loaded from `config.properties`
- Support for environment variable substitution
- Default admin password is hashed on first run

### Multi-User Data Isolation

Data isolation is enforced at multiple levels:

1. **TokenStore** stores `userId` with each token — `server/src/main/java/com/expensetracker/server/util/TokenStore.java` (lines 32-34)
2. **AuthFilter** sets `userId` as request attribute — `server/src/main/java/com/expensetracker/server/filter/AuthFilter.java` (lines 40-42)
3. **TransactionServlet** queries only by authenticated user — `server/src/main/java/com/expensetracker/server/servlet/TransactionServlet.java` (lines 27-31)
4. **DAO methods** use `...ByUser()` variants with `WHERE user_id = ?`

---

## Backup & Recovery

### How BackupServlet Works

The `server/src/main/java/com/expensetracker/server/servlet/BackupServlet.java` automatically detects the database type at runtime:

| Database | Backup Method | File Extension |
|----------|---------------|----------------|
| H2 | `SCRIPT TO` (native) | `.zip` |
| MySQL/MariaDB | JDBC-based SQL dump | `.sql` |

**Configuration:**
- Backup directory: `./backups/` (created automatically)
- Path traversal protection enabled
- Timestamps in filenames prevent overwrites

### Manual MySQL Backup (mysqldump)

```bash
# Create backup
mysqldump -u expense_user -p expense_tracker > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore backup
mysql -u expense_user -p expense_tracker < backup_20241217_120000.sql
```

### Manual H2 Backup

```sql
-- Connect to H2 console or via JDBC
SCRIPT TO '/path/to/backup.zip' COMPRESSION ZIP;

-- Restore
RUNSCRIPT FROM '/path/to/backup.zip' COMPRESSION ZIP;
```

### Recovery Steps

1. **Stop the server**
2. **Restore database:**
   - MySQL: `mysql -u user -p database < backup.sql`
   - H2: Delete `.mv.db` file and run `RUNSCRIPT`
3. **Restart server**
4. **Verify data integrity**

---

## Troubleshooting

### Common Errors

<details>
<summary><b>Port 8080 already in use</b></summary>

```bash
# Find process using port
# Windows
netstat -ano | findstr :8080
taskkill /PID <pid> /F

# Linux/macOS
lsof -i :8080
kill -9 <pid>
```
</details>

<details>
<summary><b>Database connection failed</b></summary>

1. Verify MySQL is running: `systemctl status mysql`
2. Check credentials in `config.properties`
3. Test connection: `mysql -u expense_user -p expense_tracker`
4. Check firewall allows port 3306
</details>

<details>
<summary><b>401 Unauthorized on all requests</b></summary>

1. Ensure you're including `Authorization: Bearer <token>` header
2. Check token hasn't expired (30-minute TTL)
3. Re-login to get a fresh token
</details>

<details>
<summary><b>JavaFX runtime components missing</b></summary>

Use Maven to run: `mvn javafx:run`

Or add module path manually:
```bash
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar client.jar
```
</details>

### Enable Debug Logging

Edit `shared/src/main/resources/logback.xml`:
```xml
<root level="DEBUG">
  <appender-ref ref="STDOUT" />
</root>
```

### Reset Database for Clean Tests

```bash
# H2
rm -f expense_tracker_db.mv.db expense_tracker_db.trace.db

# MySQL
mysql -u root -p -e "DROP DATABASE expense_tracker; CREATE DATABASE expense_tracker;"
mysql -u expense_user -p expense_tracker < client/src/main/resources/sql/schema.sql
```

---

## Evidence Checklist for Examiners

### Quick Verification Commands

```bash
# 1. Build passes
./mvnw clean package -DskipTests

# 2. Tests pass
./mvnw test

# 3. Server starts
java -jar server/target/server-1.0.0.jar &
sleep 3 && curl http://localhost:8080/health

# 4. Authentication works
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 5. 401 without token
curl -i http://localhost:8080/api/transactions
```

### Code Evidence Locations

| Feature | File | Lines |
|---------|------|-------|
| **BCrypt hashing** | `shared/src/main/java/com/expensetracker/util/PasswordUtil.java` | 13-30 |
| **Token TTL (30 min)** | `server/src/main/java/com/expensetracker/server/util/TokenStore.java` | 19-23 |
| **AuthFilter** | `server/src/main/java/com/expensetracker/server/filter/AuthFilter.java` | 20-50 |
| **User data isolation** | `server/src/main/java/com/expensetracker/server/servlet/TransactionServlet.java` | 27-55 |
| **HikariCP pooling** | `shared/src/main/java/com/expensetracker/util/DatabaseManager.java` | 70-90 |
| **Offline sync queue** | `client/src/main/java/com/expensetracker/client/service/SyncManager.java` | 30-50 |
| **Backup (H2/MySQL)** | `server/src/main/java/com/expensetracker/server/servlet/BackupServlet.java` | 75-130 |
| **Registration** | `server/src/main/java/com/expensetracker/server/servlet/RegisterServlet.java` | 70-130 |
| **Multi-user isolation test** | `server/src/test/java/com/expensetracker/server/IsolationTest.java` | 70-150 |

### Multi-User Isolation Test

```bash
# Run isolation test specifically
cd server && ../mvnw test -Dtest=IsolationTest
```

### Token Expiry Test

```bash
# Get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# Wait 31 minutes...
sleep 1860

# Token should be expired
curl -i http://localhost:8080/api/transactions \
  -H "Authorization: Bearer $TOKEN"
# Expected: 401 Unauthorized
```

---

## Notes for Instructors

This section maps implemented rubric items to specific code locations for quick evaluation.

### 1. Object-Oriented Programming (OOP)

| Requirement | Evidence |
|-------------|----------|
| Abstract class | `shared/src/main/java/com/expensetracker/model/Transaction.java` |
| Subclasses | `Expense.java`, `Income.java` in model package |
| Interfaces | `shared/src/main/java/com/expensetracker/dao/TransactionDAO.java`, `UserDAO.java`, `CategoryDAO.java` |
| Polymorphism | `client/src/main/java/com/expensetracker/service/ReportGenerator.java` - processes `List<Transaction>` |

### 2. Collections & Generics

| Requirement | Evidence |
|-------------|----------|
| Generic utility class | `shared/src/main/java/com/expensetracker/util/Paginator.java` - `Paginator<T>` |
| List/Map usage | `ReportGenerator.java` - `List<Transaction>`, `Map<String, BigDecimal>` |
| SortedMap | `ReportGenerator.generateMonthlySummary()` - `SortedMap<YearMonth, MonthlySummary>` |

### 3. Multithreading & Synchronization

| Requirement | Evidence |
|-------------|----------|
| ExecutorService | `client/src/main/java/com/expensetracker/ui/MainController.java` line 45 |
| JavaFX Task | `MainController.java` - `handleAddTransaction()`, `handleImportCSV()` |
| Connectivity polling | `client/src/main/java/com/expensetracker/client/service/ConnectivityManager.java` |
| Sync queue | `SyncManager.java` - background sync with retry logic |

### 4. Database & JDBC

| Requirement | Evidence |
|-------------|----------|
| DatabaseManager | `shared/src/main/java/com/expensetracker/util/DatabaseManager.java` |
| HikariCP pooling | `DatabaseManager.java` lines 70-90 |
| Prepared statements | All DAO implementations use `PreparedStatement` |
| Try-with-resources | Throughout all DAO classes |
| Custom exceptions | `shared/src/main/java/com/expensetracker/exceptions/DatabaseException.java` |

### 5. Security Features

| Requirement | Evidence |
|-------------|----------|
| BCrypt hashing | `shared/src/main/java/com/expensetracker/util/PasswordUtil.java` |
| Token authentication | `server/src/main/java/com/expensetracker/server/util/TokenStore.java` |
| Auth filter | `server/src/main/java/com/expensetracker/server/filter/AuthFilter.java` |
| Data isolation | `TransactionServlet.java` - all queries scoped by userId |

### 6. Testing

| Requirement | Evidence |
|-------------|----------|
| Unit tests | `PaginatorTest.java`, `ReportGeneratorTest.java`, `CSVExporterTest.java` |
| Integration tests | `ServerIntegrationTest.java`, `IsolationTest.java`, `SecurityIntegrationTest.java` |
| DAO tests | `TransactionDAOImplTest.java` |

---

## Changelog

### v1.0.0 (Final Release) — December 2024

#### Added
- ✅ User registration endpoint (`POST /api/auth/register`) with validation
- ✅ BCrypt password hashing (cost factor 12)
- ✅ Token TTL with 30-minute sliding expiration
- ✅ Multi-user data isolation (userId in TokenStore and all queries)
- ✅ Category isolation (user_id column, unique per user)
- ✅ Database backup supporting both MySQL and H2
- ✅ Offline sync queue with retry logic (max 3 retries)
- ✅ Integration tests for security, isolation, and registration
- ✅ Comprehensive README with curl examples

#### Fixed
- 🐛 Fixed MySQL backup using JDBC instead of `mysqldump` (no external dependency)
- 🐛 Fixed category visibility (users see only their categories + system defaults)
- 🐛 Fixed token store to include userId for proper data scoping

#### Changed
- 🔄 Upgraded to Java 21
- 🔄 Upgraded to JavaFX 21
- 🔄 Improved error messages in registration validation

---

## FAQ

<details>
<summary><b>Can I use PostgreSQL instead of MySQL?</b></summary>

The application uses standard SQL and should work with PostgreSQL. Update `config.properties`:
```properties
db.url=jdbc:postgresql://localhost:5432/expense_tracker
```
Add PostgreSQL driver dependency to `shared/pom.xml`.
</details>

<details>
<summary><b>How do I change the token expiration time?</b></summary>

Edit `server/src/main/java/com/expensetracker/server/util/TokenStore.java`:
```java
private static final long TOKEN_TTL_MS = 60 * 60 * 1000; // 60 minutes
```
Rebuild and restart the server.
</details>

<details>
<summary><b>Why do new users start with no transactions?</b></summary>

By design, each user starts fresh. This ensures data isolation and prevents test data leakage. Use the sample_data.sql script or the client UI to add transactions.
</details>

<details>
<summary><b>How do I run only the server without the GUI?</b></summary>

```bash
java -jar server/target/server-1.0.0.jar
```
Use curl or any REST client to interact with the API.
</details>

---

## Contribution & License

### How to Contribute

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/amazing-feature`
3. **Commit** changes: `git commit -m 'Add amazing feature'`
4. **Push** to branch: `git push origin feature/amazing-feature`
5. **Open** a Pull Request

### Code Style

- Follow standard Java conventions
- Add JavaDoc for public methods
- Include unit tests for new features
- Run `./mvnw test` before submitting PR

### Running Formatter (if configured)

```bash
# Using spotless (if added to pom.xml)
./mvnw spotless:apply
```

### License

This project is created for educational purposes as part of a university assignment.

> To use a different license, replace this section with your chosen license (e.g., MIT, Apache 2.0) and add a `LICENSE` file.

---

## Contact & Credits

**Maintainer:** `Ayushman Mishra` — `Team Lead`
                `Akshay Kumar` — `Member`
                `Daksh Sharma` — `Member`

**Course:** Personal Expense Tracker 

---

## Appendix

### SQL Schema Reference

<details>
<summary>Click to expand full schema</summary>

```sql
-- Users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Categories table (with user isolation)
CREATE TABLE IF NOT EXISTS categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,  -- NULL for system categories
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_category_per_user (user_id, name, type)
);

-- Transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    description VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    category_id INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Sync queue (client-side)
CREATE TABLE IF NOT EXISTS sync_queue (
    id INT AUTO_INCREMENT PRIMARY KEY,
    operation_type VARCHAR(20) NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    payload TEXT,
    timestamp BIGINT,
    retry_count INT DEFAULT 0
);
```

</details>

### Example Environment File (.env)

```bash
# .env (add to .gitignore!)
DB_URL=jdbc:mysql://localhost:3306/expense_tracker
DB_USER=expense_user
DB_PASS=your_secure_password_here
```

### Default Categories

| Name | Type | Description |
|------|------|-------------|
| Salary | INCOME | Monthly salary |
| Freelance | INCOME | Freelance work |
| Food | EXPENSE | Groceries and dining |
| Transport | EXPENSE | Public transport and fuel |
| Utilities | EXPENSE | Electricity, water, internet |
| Family | EXPENSE | Family related expenses |
| Health | EXPENSE | Healthcare and medical |
| Education | EXPENSE | School and learning |
| Entertainment | EXPENSE | Movies, games, and fun |
| Shopping | EXPENSE | Clothing and electronics |
| Other | EXPENSE | Miscellaneous expenses |

---

<div align="center">

**[⬆ Back to Top](#personal-expense-tracker)**

</div>

# Personal Expense Tracker

## Technical Documentation

---

**Project Name:** Personal Expense Tracker  
**Type:** Full-Stack Java Application  
**Submission Type:** Final Academic Project  
**Version:** 1.0.0

---

## Table of Contents

1. [Abstract](#1-abstract)
2. [Problem Statement](#2-problem-statement)
3. [Objectives](#3-objectives)
4. [Technology Stack](#4-technology-stack)
5. [System Architecture](#5-system-architecture)
6. [Module Description](#6-module-description)
7. [Data Validation & Error Handling](#7-data-validation--error-handling)
8. [Security Implementation](#8-security-implementation)
9. [Database Design](#9-database-design)
10. [Innovation & Extra Features](#10-innovation--extra-features)
11. [Build & Execution Instructions](#11-build--execution-instructions)
12. [Testing & Validation](#12-testing--validation)
13. [Screenshots](#13-screenshots)
14. [Limitations & Future Enhancements](#14-limitations--future-enhancements)
15. [Conclusion](#15-conclusion)

---

## 1. Abstract

The Personal Expense Tracker is a multi-user financial management application developed using Java technologies, designed to help individuals track their expenses and income efficiently. The system employs a client-server architecture with a JavaFX desktop client providing an intuitive graphical interface, and an embedded Jetty server exposing a RESTful API for data management. The application supports secure multi-user authentication using BCrypt password hashing and UUID-based token authentication, ensuring user data isolation. Key features include transaction management, category-based organization, CSV import/export capabilities, database backup functionality, and offline/online synchronization. The project demonstrates object-oriented programming principles, design patterns, and modern Java practices including Java 21 features, HikariCP connection pooling, and comprehensive exception handling.

---

## 2. Problem Statement

Managing personal finances effectively remains a challenge for many individuals. Common problems include:

- **Lack of Organization:** Many people struggle to categorize and track their daily expenses and income sources, leading to poor financial visibility.

- **Data Accessibility:** Traditional methods like spreadsheets or paper records lack the convenience of a dedicated application with filtering, reporting, and visualization capabilities.

- **Multi-Device Limitations:** Single-user desktop applications do not support multiple users or data synchronization across different contexts.

- **Security Concerns:** Financial data is sensitive, and many simple tracking solutions do not implement proper authentication or data isolation mechanisms.

- **Offline Functionality:** Users need to track expenses even when network connectivity is unavailable, with seamless synchronization when connectivity is restored.

This project addresses these challenges by providing a secure, feature-rich expense tracking application that supports multiple users with strictly isolated data, offers both online and offline functionality, and provides comprehensive reporting and data management capabilities.

---

## 3. Objectives

The primary technical objectives of this project are:

1. **Implement a Client-Server Architecture:** Develop a modular application with clear separation between the JavaFX GUI client and the Jetty-based REST API server, enabling scalability and maintainability.

2. **Ensure Secure Multi-User Support:** Implement robust authentication using BCrypt password hashing (cost factor 12) and token-based session management with automatic expiration (30-minute TTL).

3. **Enforce User Data Isolation:** Design and implement database schemas and DAO layer queries that strictly scope all financial data to the authenticated user, preventing cross-user data leakage.

4. **Provide Comprehensive Transaction Management:** Enable users to create, read, update, and delete transactions with support for filtering by date range and category.

5. **Support Offline Operations:** Implement a sync queue mechanism that stores operations locally when the server is unreachable and synchronizes them automatically when connectivity is restored.

6. **Enable Data Portability:** Provide CSV import/export functionality for bulk data operations and database backup capabilities for both H2 and MySQL databases.

---

## 4. Technology Stack

### Programming Language

| Component | Technology | Version |
|-----------|------------|---------|
| Core Language | Java | 21 (LTS) |
| Build Configuration | Maven | 3.6+ |

### Frameworks and Libraries

| Category | Technology | Version | Purpose |
|----------|------------|---------|---------|
| GUI Framework | JavaFX | 21 | Desktop user interface with FXML |
| HTTP Server | Eclipse Jetty | 11.0.15 | Embedded servlet container |
| JSON Processing | Jackson | 2.15.2 | JSON serialization/deserialization |
| Connection Pooling | HikariCP | 5.0.1 | High-performance JDBC connection pool |
| Password Security | jBCrypt | 0.4 | BCrypt password hashing |
| Logging | SLF4J + Logback | 2.0.9 / 1.4.11 | Structured logging |

### Database

| Database | Version | Use Case |
|----------|---------|----------|
| H2 Database | 2.2.224 | Development, testing, and embedded deployment |
| MySQL | 8.0+ | Production deployment |

### Build Tools

| Tool | Purpose |
|------|---------|
| Apache Maven | Dependency management and build automation |
| Maven Wrapper (mvnw) | Consistent Maven version across environments |
| Maven Shade Plugin | Creating executable JAR with dependencies |
| JavaFX Maven Plugin | Running JavaFX applications |

### Version Control

| Tool | Purpose |
|------|---------|
| Git | Source code version control |

---

## 5. System Architecture

### High-Level Architecture

The Personal Expense Tracker follows a three-tier client-server architecture:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              PRESENTATION TIER                              │
│                            (JavaFX Client Module)                           │
│  ┌───────────────┐  ┌───────────────┐  ┌─────────────────────────────────┐  │
│  │ UI Controllers│  │ SyncManager   │  │ ConnectivityManager             │  │
│  │ (Login, Main) │  │ (Offline Ops) │  │ (Network Monitoring)            │  │
│  └───────────────┘  └───────────────┘  └─────────────────────────────────┘  │
│           │                  │                         │                    │
│           └──────────────────┼─────────────────────────┘                    │
│                              ▼                                              │
│                    ┌─────────────────┐                                      │
│                    │ RemoteRepository│ ◄── java.net.http.HttpClient         │
│                    └─────────────────┘                                      │
└──────────────────────────────┼──────────────────────────────────────────────┘
                               │ REST API (JSON over HTTP)
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              APPLICATION TIER                               │
│                           (Jetty Server Module)                             │
│  ┌───────────────┐  ┌───────────────────────────────────────────────────┐   │
│  │  AuthFilter   │──│ Servlets: AuthServlet, RegisterServlet,           │   │
│  │  (Security)   │  │   TransactionServlet, CategoryServlet,            │   │
│  └───────────────┘  │   BackupServlet                                   │   │
│           │         └───────────────────────────────────────────────────┘   │
│           ▼                              │                                  │
│  ┌───────────────┐              ┌─────────────────┐                         │
│  │  TokenStore   │              │   DAO Layer     │                         │
│  │  (Sessions)   │              │ (UserDAO,       │                         │
│  │  with TTL     │              │  TransactionDAO,│                         │
│  └───────────────┘              │  CategoryDAO)   │                         │
│                                 └─────────────────┘                         │
│                                          │                                  │
│                                          ▼                                  │
│                                 ┌─────────────────┐                         │
│                                 │ DatabaseManager │ ◄── HikariCP Pool       │
│                                 │ (Singleton)     │     (10 connections)    │
│                                 └─────────────────┘                         │
└──────────────────────────────────────────┼──────────────────────────────────┘
                                           │ JDBC
                                           ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                DATA TIER                                    │
│                         (MySQL 8.0+ or H2 Database)                         │
│  ┌─────────┐    ┌──────────────┐    ┌──────────────┐                        │
│  │  users  │◄───│  categories  │◄───│ transactions │                        │
│  └─────────┘    └──────────────┘    └──────────────┘                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Module Structure

The project is organized as a Maven multi-module project:

```
PET2/
├── pom.xml                     # Parent POM (dependency management)
├── shared/                     # Shared module (models, DAOs, utilities)
│   ├── pom.xml
│   └── src/main/java/com/expensetracker/
│       ├── model/              # Transaction, Category, User, Expense, Income
│       ├── dao/                # DAO interfaces and implementations
│       ├── util/               # DatabaseManager, PasswordUtil, JsonUtil
│       └── exceptions/         # DatabaseException, ValidationException
│
├── server/                     # Server module (REST API)
│   ├── pom.xml
│   └── src/main/java/com/expensetracker/server/
│       ├── ServerApp.java      # Jetty bootstrap
│       ├── filter/             # AuthFilter (security)
│       ├── util/               # TokenStore (session management)
│       └── servlet/            # REST endpoint handlers
│
└── client/                     # Client module (JavaFX GUI)
    ├── pom.xml
    └── src/main/java/com/expensetracker/
        ├── MainApp.java        # JavaFX entry point
        ├── ui/                 # FXML controllers
        ├── client/api/         # RemoteRepository (HTTP client)
        ├── client/service/     # SyncManager, ConnectivityManager
        └── service/            # CSVExporter, CSVImporter, ReportGenerator
```

### Communication Flow

#### Authentication Flow

1. Client sends `POST /api/auth/login` with username and password
2. Server's `AuthServlet` invokes `UserDAO.authenticate()`
3. `UserDAOImpl` verifies password using `BCrypt.checkpw()`
4. Server generates UUID token and stores in `TokenStore` with user ID
5. Server responds with token, user details, and user ID
6. Client stores token and includes `Authorization: Bearer <token>` in subsequent requests
7. `AuthFilter` intercepts all `/api/*` requests (except `/api/auth/*`)
8. Filter validates token via `TokenStore.isValid()` and sets `userId` request attribute
9. Servlets use `userId` attribute to scope all database queries

#### Offline Synchronization Flow

1. `ConnectivityManager` monitors server reachability (5-second polling interval)
2. When offline, CRUD operations are queued in `sync_queue` table via `SyncManager`
3. When connectivity is restored, `SyncManager.sync()` processes queued operations
4. Successful operations are removed from queue; failed operations retry up to 3 times

---

## 6. Module Description

### 6.1 Authentication Module

**Location:** `server/src/main/java/com/expensetracker/server/servlet/AuthServlet.java`

The Authentication Module handles user login through a secure token-based system:

- **Endpoint:** `POST /api/auth/login`
- **Input Validation:** Checks for presence of username and password fields
- **Password Verification:** Uses BCrypt algorithm via `PasswordUtil.verifyPassword()`
- **Token Generation:** Creates UUID-based tokens stored in `TokenStore` with 30-minute TTL
- **Response:** Returns JSON containing token, user object, and user ID
- **Sliding Expiration:** Token TTL is refreshed on each successful validation

**Key Implementation Details:**
- `TokenStore` is thread-safe using `ConcurrentHashMap`
- Background cleanup thread removes expired tokens every 5 minutes
- Each token entry stores username and user ID for data isolation enforcement

### 6.2 User Registration Module

**Location:** `server/src/main/java/com/expensetracker/server/servlet/RegisterServlet.java`

The Registration Module manages new user account creation:

- **Endpoint:** `POST /api/auth/register`
- **Validation Rules:**
  - Username: 3-30 characters, alphanumeric and underscores only (regex: `^[a-zA-Z0-9_]{3,30}$`)
  - Email: Optional but validated if provided (RFC-like pattern)
  - Password: Minimum 6 characters
- **Duplicate Detection:** Checks username uniqueness before insertion
- **Password Security:** Raw password sent to DAO layer where BCrypt hashing occurs
- **Response Codes:**
  - `201 Created`: Successful registration
  - `400 Bad Request`: Validation failure
  - `409 Conflict`: Username already exists

### 6.3 Transaction Management Module

**Location:** `server/src/main/java/com/expensetracker/server/servlet/TransactionServlet.java`

The Transaction Management Module provides full CRUD operations for financial transactions:

- **Endpoints:**
  - `GET /api/transactions` - Retrieve transactions with optional filters
  - `POST /api/transactions` - Create new transaction
  - `PUT /api/transactions` - Update existing transaction
  - `DELETE /api/transactions/{id}` - Delete transaction

- **Filtering Capabilities:**
  - Date range filtering (`startDate`, `endDate` parameters)
  - Category filtering (`categoryId` parameter)
  - Combined date range and category filtering

- **Data Isolation:**
  - All queries include `user_id` condition
  - User ID extracted from request attributes (set by AuthFilter)
  - Methods like `findAllByUser(userId)`, `deleteByUser(id, userId)` ensure isolation

- **Transaction Types:**
  - `Expense` - Represents money spent
  - `Income` - Represents money received
  - Both inherit from abstract `Transaction` class demonstrating polymorphism

### 6.4 Category Management Module

**Location:** `server/src/main/java/com/expensetracker/server/servlet/CategoryServlet.java`

The Category Management Module handles expense/income categorization:

- **Endpoints:**
  - `GET /api/categories` - Retrieve user's categories plus system defaults
  - `POST /api/categories` - Create user-specific category
  - `PUT /api/categories` - Update category
  - `DELETE /api/categories/{id}` - Delete category

- **Category Types:**
  - System categories (`user_id = NULL`) - Shared defaults like "Salary", "Food", "Transport"
  - User categories (`user_id = <id>`) - Custom categories created by individual users

- **Default Categories:**
  - Income: Salary, Freelance
  - Expense: Food, Transport, Utilities, Family, Health, Education, Entertainment, Shopping, Other

### 6.5 Offline/Online Synchronization Module

**Location:** `client/src/main/java/com/expensetracker/client/service/`

The Synchronization Module enables seamless offline operation:

**ConnectivityManager:**
- Monitors server reachability via periodic health checks (5-second interval)
- Maintains connection state (`ONLINE` / `OFFLINE`)
- Notifies registered listeners of state changes
- Runs on daemon thread to avoid blocking application shutdown

**SyncManager:**
- Maintains local `sync_queue` table for pending operations
- Queues INSERT, UPDATE, DELETE operations when offline
- Processes queue when connectivity is restored
- Implements retry logic with maximum 3 attempts per operation
- Provides callback mechanism (`onSyncComplete`) for UI updates

**Sync Queue Schema:**
```sql
CREATE TABLE sync_queue (
    id INT AUTO_INCREMENT PRIMARY KEY,
    operation_type VARCHAR(20) NOT NULL,  -- INSERT, UPDATE, DELETE
    entity_type VARCHAR(20) NOT NULL,      -- TRANSACTION, CATEGORY
    payload TEXT,                          -- JSON serialized entity
    timestamp BIGINT,                      -- Operation timestamp
    retry_count INT DEFAULT 0              -- Failure retry counter
)
```

### 6.6 Backup & Export Module

**Location:** `server/src/main/java/com/expensetracker/server/servlet/BackupServlet.java`

The Backup Module provides database backup functionality:

- **Endpoint:** `POST /api/backup`
- **Database Detection:** Runtime detection of database type via JDBC metadata
- **H2 Backup:** Uses native `SCRIPT TO` command with ZIP compression
- **MySQL Backup:** JDBC-based SQL dump generation (no external tools required)
- **Security:** Fixed backup directory prevents path traversal attacks
- **Output:** Timestamped backup files in `backups/` directory

**CSV Export (Client-Side):**
- Location: `client/src/main/java/com/expensetracker/service/CSVExporter.java`
- Exports transactions to CSV format with proper escaping
- Headers: ID, Type, Amount, Description, Date, Category

**CSV Import (Client-Side):**
- Location: `client/src/main/java/com/expensetracker/service/CSVImporter.java`
- Parses CSV files handling quoted fields and escaped characters
- Validates data format and provides line-specific error reporting

---

## 7. Data Validation & Error Handling

### 7.1 Client-Side Validation

**Form Validation (MainController):**
- Validates all required fields are populated before submission
- Checks numeric format for amount field using `BigDecimal` parsing
- Validates date selection is not null
- Displays user-friendly error alerts via JavaFX `Alert` dialogs

**Input Sanitization:**
- CSV export escapes special characters (commas, quotes, newlines)
- CSV import handles quoted field parsing correctly

### 7.2 Server-Side Validation

**RegisterServlet Validation:**
```java
// Username validation pattern
private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");
// Email validation pattern
private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
// Minimum password length
private static final int MIN_PASSWORD_LENGTH = 6;
```

**UserDAOImpl Validation:**
- Validates username format before database insertion
- Validates email format if provided
- Enforces minimum password length requirement
- Throws `ValidationException` for invalid inputs

**Transaction Validation:**
- Requires amount, description, date, and category ID
- Validates numeric amount format
- Ensures category exists before foreign key insertion

### 7.3 Exception Handling Strategy

**Custom Exceptions:**

1. **DatabaseException** (`shared/src/main/java/com/expensetracker/exceptions/`)
   - Wraps `SQLException` with meaningful messages
   - Used for all database-related errors
   - Propagates through DAO layer to servlets

2. **ValidationException** (`shared/src/main/java/com/expensetracker/exceptions/`)
   - Thrown for business rule violations
   - Contains user-readable error messages
   - Used for input validation failures

**Servlet Error Handling:**
```java
try {
    // Business logic
} catch (DatabaseException e) {
    logger.error("Database error", e);
    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    resp.getWriter().write(JsonUtil.toJson(Map.of("error", e.getMessage())));
} catch (Exception e) {
    logger.error("Unexpected error", e);
    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    resp.getWriter().write(JsonUtil.toJson(Map.of("error", "Internal server error")));
}
```

**Client-Side Error Handling:**
- `CompletableFuture.exceptionally()` for async operation failures
- `Platform.runLater()` for UI thread-safe error display
- Graceful degradation to offline mode on connectivity loss

### 7.4 Crash Prevention

- **Connection Pool Management:** HikariCP prevents connection exhaustion with 10 max connections
- **Resource Cleanup:** Try-with-resources ensures JDBC resources are properly closed
- **Thread Safety:** `ConcurrentHashMap` in TokenStore prevents race conditions
- **Daemon Threads:** Background tasks use daemon threads to allow clean JVM shutdown
- **Null Safety:** Explicit null checks before operations on nullable values

---

## 8. Security Implementation

### 8.1 Password Hashing with BCrypt

**Location:** `shared/src/main/java/com/expensetracker/util/PasswordUtil.java`

```java
// BCrypt work factor (cost) - 12 provides strong security
private static final int BCRYPT_ROUNDS = 12;

public static String hashPassword(String plainPassword) {
    return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
}

public static boolean verifyPassword(String plainPassword, String hashedPassword) {
    return BCrypt.checkpw(plainPassword, hashedPassword);
}
```

**Security Properties:**
- Cost factor of 12 provides ~300ms hashing time (resistant to brute force)
- Automatic salt generation and storage within hash
- Timing-safe comparison prevents timing attacks
- Fallback to plain comparison for legacy password migration

### 8.2 Token-Based Authentication

**Location:** `server/src/main/java/com/expensetracker/server/util/TokenStore.java`

**Token Properties:**
- UUID-based tokens (128-bit random)
- 30-minute Time-To-Live (TTL)
- Sliding expiration (refreshed on each use)
- Thread-safe storage using `ConcurrentHashMap`

**Token Entry Structure:**
```java
private static class TokenEntry {
    final String username;
    final int userId;      // For data isolation
    long lastAccessed;     // For TTL calculation
}
```

**Automatic Cleanup:**
- Background scheduler runs every 5 minutes
- Removes all expired tokens from memory
- Uses daemon thread for clean shutdown

### 8.3 AuthFilter Security

**Location:** `server/src/main/java/com/expensetracker/server/filter/AuthFilter.java`

**Filter Configuration:**
- Applied to all `/api/*` endpoints
- Excludes `/api/auth/*` (login and registration)
- Intercepts requests before servlet processing

**Authentication Process:**
1. Extract `Authorization` header
2. Validate `Bearer <token>` format
3. Check token validity in `TokenStore`
4. Set `username` and `userId` as request attributes
5. Continue filter chain or return 401 Unauthorized

**Response on Failure:**
```json
{
    "error": "Unauthorized access"
}
```

### 8.4 User Data Isolation Strategy

**Database-Level Isolation:**
- All transactions have `user_id` foreign key
- Categories are user-scoped (or system-wide if `user_id = NULL`)
- Unique constraints include user_id: `UNIQUE (user_id, name, type)`

**Query-Level Isolation:**
- All DAO methods require user ID parameter
- SQL queries always include `WHERE user_id = ?` clause
- Methods named `...ByUser()` enforce isolation

**Example Query (TransactionDAOImpl):**
```sql
SELECT t.*, c.name as category_name 
FROM transactions t 
LEFT JOIN categories c ON t.category_id = c.id 
WHERE t.user_id = ? 
ORDER BY t.date DESC
```

**Servlet Enforcement:**
- User ID extracted from request attributes (set by AuthFilter)
- All CRUD operations pass user ID to DAO methods
- Attempting to access another user's data returns empty results or fails

---

## 9. Database Design

### 9.1 Entity-Relationship Diagram

```
┌─────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│     users       │       │   categories     │       │  transactions    │
├─────────────────┤       ├──────────────────┤       ├──────────────────┤
│ id (PK)         │◄──┬───│ id (PK)          │◄──────│ id (PK)          │
│ username (UQ)   │   │   │ user_id (FK)     │       │ user_id (FK)     │
│ password        │   │   │ name             │       │ amount           │
│ email           │   │   │ type             │       │ description      │
│ created_at      │   │   │ description      │       │ date             │
└─────────────────┘   │   │ created_at       │       │ category_id (FK) │
                      │   └──────────────────┘       │ type             │
                      │            ▲                 │ created_at       │
                      │            │                 └──────────────────┘
                      └────────────┴─────────────────────────┘
```

### 9.2 Table Definitions

**Users Table:**
```sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,        -- BCrypt hash
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Categories Table:**
```sql
CREATE TABLE categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,                           -- NULL for system categories
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,             -- 'EXPENSE' or 'INCOME'
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_category_per_user (user_id, name, type)
);
```

**Transactions Table:**
```sql
CREATE TABLE transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    description VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    category_id INT NOT NULL,
    type VARCHAR(20) NOT NULL,             -- 'EXPENSE' or 'INCOME'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_date (date),
    INDEX idx_category (category_id),
    INDEX idx_type (type)
);
```

### 9.3 Key Fields and Relationships

| Relationship | Type | Description |
|--------------|------|-------------|
| users → categories | One-to-Many | Each user can have multiple custom categories |
| users → transactions | One-to-Many | Each user can have multiple transactions |
| categories → transactions | One-to-Many | Each category can have multiple transactions |

**Foreign Key Constraints:**
- `categories.user_id` → `users.id` (CASCADE DELETE)
- `transactions.user_id` → `users.id`
- `transactions.category_id` → `categories.id` (RESTRICT DELETE)

### 9.4 H2 vs MySQL Usage

| Aspect | H2 Database | MySQL Database |
|--------|-------------|----------------|
| Use Case | Development, Testing, Embedded | Production |
| Configuration | `jdbc:h2:./expense_tracker_db` | `jdbc:mysql://localhost:3306/expense_tracker` |
| Setup | Zero configuration (auto-created) | Requires server installation |
| Mode | MySQL compatibility mode enabled | Native |
| Backup | `SCRIPT TO` command (ZIP) | JDBC-based SQL dump |
| Persistence | File-based or in-memory | Server-based |

**Switching Databases:**
Update `shared/src/main/resources/config.properties`:
```properties
# For H2:
db.url=jdbc:h2:./expense_tracker_db;DB_CLOSE_DELAY=-1;MODE=MySQL
db.user=sa
db.password=

# For MySQL:
db.url=jdbc:mysql://localhost:3306/expense_tracker?useSSL=false&serverTimezone=UTC
db.user=expense_user
db.password=your_password
```

---

## 10. Innovation & Extra Features

### 10.1 Offline Mode and Sync Queue

The application implements a sophisticated offline-first architecture:

**Connectivity Detection:**
- `ConnectivityManager` performs server health checks every 5 seconds
- Status changes trigger UI updates (Online/Offline indicator)
- State machine pattern ensures clean transitions

**Operation Queueing:**
- When offline, all CRUD operations are serialized to JSON
- Operations stored in local `sync_queue` table with timestamps
- Queue preserves operation order for consistency

**Synchronization:**
- Automatic sync triggered when connectivity is restored
- Sequential processing maintains data integrity
- Failed operations retry up to 3 times before removal
- Success callback refreshes UI with synchronized data

### 10.2 CSV Import/Export

**Export Features:**
- Full transaction history export
- Proper CSV escaping for special characters
- Includes all fields: ID, Type, Amount, Description, Date, Category

**Import Features:**
- Bulk transaction import from CSV files
- Handles quoted fields with embedded commas
- Line-by-line error reporting for debugging
- Progress tracking for large files

### 10.3 Database Backup

**Multi-Database Support:**
- Runtime database detection via JDBC metadata
- H2: Native `SCRIPT TO` command with ZIP compression
- MySQL: Pure JDBC SQL dump (no mysqldump dependency)

**Security Measures:**
- Fixed backup directory prevents path traversal
- Timestamped filenames prevent overwrites
- Filename sanitization removes special characters

**Backup Content:**
- Complete table structure (CREATE TABLE)
- All data (INSERT statements)
- Foreign key handling (SET FOREIGN_KEY_CHECKS)

### 10.4 Additional Features

**Report Generation:**
- Total income/expense calculations
- Net balance computation
- Monthly summaries with YearMonth grouping
- Category breakdown for expense analysis
- Daily totals for trend visualization

**Generic Pagination:**
- `Paginator<T>` class for any collection type
- Page navigation (next, previous, go to)
- Page size configuration
- Empty page handling

**Connection Pooling:**
- HikariCP with 10 maximum connections
- 2 minimum idle connections
- 30-second connection timeout
- Leak detection at 60 seconds

---

## 11. Build & Execution Instructions

### 11.1 Prerequisites

| Requirement | Version | Verification Command |
|-------------|---------|---------------------|
| Java JDK | 21+ | `java -version` |
| Maven | 3.6+ | `mvn -version` |
| MySQL (optional) | 8.0+ | `mysql --version` |

### 11.2 Building the Project

**Clone and Build:**
```bash
git clone <repository-url>
cd PET2
./mvnw clean package
```

**Expected Output:**
```
[INFO] Reactor Summary:
[INFO] Personal Expense Tracker Parent .................... SUCCESS
[INFO] shared ............................................. SUCCESS
[INFO] server ............................................. SUCCESS
[INFO] client ............................................. SUCCESS
[INFO] BUILD SUCCESS
```

**Skip Tests (faster build):**
```bash
./mvnw clean package -DskipTests
```

### 11.3 Running the Server

**Option 1: Using Executable JAR (Recommended)**
```bash
java -jar server/target/server-1.0.0.jar
```

**Option 2: From Source**
```bash
cd server
../mvnw exec:java -Dexec.mainClass=com.expensetracker.server.ServerApp
```

**Expected Output:**
```
Initializing Database...
Starting server on port 8080...
```

**Verify Server:**
```bash
curl http://localhost:8080/health
# Response: Server is running
```

### 11.4 Running the Client

**Start JavaFX Client:**
```bash
cd client
../mvnw javafx:run
```

**Default Credentials:**
- Username: `admin`
- Password: `admin123`

### 11.5 Configuration Files

**Primary Configuration:**
`shared/src/main/resources/config.properties`
```properties
# Database Configuration
db.url=jdbc:h2:./expense_tracker_db;DB_CLOSE_DELAY=-1;MODE=MySQL
db.user=sa
db.password=
```

**Logging Configuration:**
`shared/src/main/resources/logback.xml`

**Production Environment Variables:**
```powershell
# Windows PowerShell
$env:DB_USER="expense_user"
$env:DB_PASS="your_secure_password"
```

---

## 12. Testing & Validation

### 12.1 Test Organization

The project includes comprehensive test suites across all modules:

```
PET2/
├── server/src/test/java/com/expensetracker/server/
│   ├── IsolationTest.java              # User data isolation verification
│   ├── SecurityIntegrationTest.java    # Authentication/authorization tests
│   ├── ServerIntegrationTest.java      # Full server integration tests
│   └── RegistrationIntegrationTest.java # User registration flow tests
│
├── client/src/test/java/com/expensetracker/
│   ├── client/ClientIntegrationTest.java
│   ├── service/CSVExporterTest.java
│   ├── service/ReportGeneratorTest.java
│   ├── util/PaginatorTest.java
│   └── dao/impl/TransactionDAOImplTest.java
│
└── shared/src/test/java/com/expensetracker/dao/
    └── TransactionDAOTest.java
```

### 12.2 Unit Tests

**CSVExporterTest:**
- Tests CSV header generation
- Validates field escaping for special characters
- Verifies output file format

**ReportGeneratorTest:**
- Tests total income calculation
- Tests total expense calculation
- Tests net balance computation
- Validates monthly summary grouping

**PaginatorTest:**
- Tests page navigation (next, previous)
- Tests boundary conditions (first/last page)
- Tests empty collection handling
- Validates page size configuration

### 12.3 Integration Tests

**IsolationTest:**
- Creates two users (User A and User B)
- User A creates a transaction
- Verifies User B cannot see User A's transaction
- Validates data isolation enforcement

**SecurityIntegrationTest:**
- Tests unauthorized access returns 401
- Tests authorized access with valid token returns 200
- Validates AuthFilter behavior

**ServerIntegrationTest:**
- Full CRUD operation testing
- API endpoint response validation
- Error handling verification

**RegistrationIntegrationTest:**
- New user registration flow
- Duplicate username detection
- Validation error handling

### 12.4 Test Scenarios Covered

| Category | Scenario | Status |
|----------|----------|--------|
| Authentication | Valid login | ✓ |
| Authentication | Invalid credentials | ✓ |
| Authentication | Missing token | ✓ |
| Authentication | Expired token | ✓ |
| Registration | Valid registration | ✓ |
| Registration | Duplicate username | ✓ |
| Registration | Invalid input format | ✓ |
| Data Isolation | Cross-user access prevention | ✓ |
| Transactions | CRUD operations | ✓ |
| Transactions | Date range filtering | ✓ |
| Categories | User-scoped categories | ✓ |
| Utilities | CSV export/import | ✓ |
| Utilities | Pagination | ✓ |
| Reports | Financial calculations | ✓ |

### 12.5 Running Tests

**Run All Tests:**
```bash
./mvnw test
```

**Run Specific Module Tests:**
```bash
./mvnw test -pl server
./mvnw test -pl client
./mvnw test -pl shared
```

---

## 13. Screenshots

The following screenshots document the application's user interface:

1. **Login Screen** - User authentication form with username and password fields
2. **Main Dashboard** - Transaction list with summary statistics and charts
3. **Add Transaction** - Form for creating new expense or income entries
4. **Category Management** - Interface for viewing and creating categories
5. **Reports View** - Pie charts and line charts for expense analysis
6. **CSV Import Progress** - Progress indicator during bulk data import
7. **Offline Mode Indicator** - Status display showing connectivity state

*Note: Screenshots should be captured from a running instance of the application and placed in the `docs/images/` directory.*

---

## 14. Limitations & Future Enhancements

### 14.1 Current Limitations

1. **Single Server Instance:** The current architecture runs a single server instance; horizontal scaling would require session sharing mechanism for token store.

2. **No Password Reset:** Users cannot reset forgotten passwords; this requires email integration not currently implemented.

3. **Limited Reporting:** Charts are basic; advanced analytics like trend prediction or budget tracking are not implemented.

4. **No Data Encryption at Rest:** Database contents are not encrypted; sensitive data relies on database-level security.

5. **Desktop Only:** No mobile or web client; JavaFX limits deployment to desktop platforms.

6. **Manual Category Mapping in CSV Import:** Imported transactions require manual category assignment as CSV category names may not match existing categories.

7. **No Recurring Transactions:** Users must manually enter repeated transactions; no scheduling functionality exists.

### 14.2 Potential Improvements

1. **Web Client:** Develop a React or Angular web frontend for browser-based access.

2. **Mobile Application:** Create Android/iOS clients using the existing REST API.

3. **Enhanced Security:**
   - Implement refresh tokens for longer sessions
   - Add two-factor authentication (2FA)
   - Encrypt sensitive data at rest

4. **Advanced Features:**
   - Budget planning and tracking
   - Recurring transaction scheduling
   - Multiple currency support
   - Bank statement import (OFX/QIF)

5. **Scalability:**
   - Redis-based token store for distributed deployment
   - Database read replicas for reporting
   - Containerization with Docker/Kubernetes

6. **User Experience:**
   - Dark mode theme
   - Customizable dashboard widgets
   - Export to PDF reports
   - Data visualization improvements

7. **DevOps:**
   - CI/CD pipeline integration
   - Automated deployment scripts
   - Monitoring and alerting

---

## 15. Conclusion

The Personal Expense Tracker project successfully demonstrates the development of a complete, production-ready financial management application using modern Java technologies. The implementation achieves all stated objectives:

**Key Achievements:**

1. **Robust Architecture:** The multi-module Maven project with clear separation between client, server, and shared components enables maintainability and scalability.

2. **Secure Multi-User Support:** BCrypt password hashing with cost factor 12 and UUID-based token authentication with automatic expiration provide industry-standard security.

3. **Data Isolation:** Comprehensive user data isolation at both the database schema level and query level prevents any cross-user data leakage.

4. **Feature Completeness:** Full CRUD operations for transactions and categories, combined with filtering, reporting, and data import/export capabilities, deliver a complete expense tracking solution.

5. **Offline Resilience:** The sync queue mechanism enables uninterrupted operation during network outages with automatic synchronization on reconnection.

6. **Code Quality:** Consistent use of design patterns (Singleton, DAO, Factory), proper exception handling, logging, and comprehensive testing demonstrates professional software engineering practices.

The project serves as an effective demonstration of object-oriented programming principles, client-server architecture, REST API design, database management, and security implementation in Java. The modular design allows for future enhancements while maintaining backward compatibility with existing deployments.

---

*Document Version: 1.0*  
*Last Updated: December 2024*

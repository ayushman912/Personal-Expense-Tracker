# Personal Expense Tracker

A comprehensive JavaFX application for tracking personal expenses and income, built with Java 17+, Maven, JavaFX, and MySQL.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Setup Instructions](#setup-instructions)
- [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [Rubric Mapping](#rubric-mapping)
- [Testing](#testing)
- [Database Schema](#database-schema)

## Features

- **User Authentication**: Simple login/registration system
- **Transaction Management**: Add, update, delete expenses and income
- **Category Management**: CRUD operations for transaction categories
- **Filtering**: Filter transactions by date range and/or category
- **Financial Reports**: Monthly summaries, category breakdowns
- **Charts & Visualization**: Pie charts for category breakdown, line charts for monthly trends
- **CSV Import/Export**: Import and export transactions to/from CSV files
- **Bulk Import**: Demo import with 10,000 rows and progress tracking
- **Database Backup**: SQL dump functionality

## Prerequisites

- **Java 17 or higher** (JDK)
- **Maven 3.6+**
- **MySQL 8.0+** (or compatible database)
- **IDE** (IntelliJ IDEA, Eclipse, or VS Code recommended)

## Setup Instructions

### 1. Database Setup

1. Start MySQL server:
   ```bash
   # Windows
   net start MySQL80
   
   # Linux/Mac
   sudo systemctl start mysql
   ```

2. Create database and load schema:
   ```bash
   mysql -u root -p < src/main/resources/sql/schema.sql
   ```

3. Load sample data (optional):
   ```bash
   mysql -u root -p < src/main/resources/sql/sample_data.sql
   ```

4. Update database credentials in `DatabaseManager.java` if needed:
   ```java
   private static final String DB_URL = "jdbc:mysql://localhost:3306/expense_tracker?useSSL=false&serverTimezone=UTC";
   private static final String DB_USER = "root";
   private static final String DB_PASSWORD = "root";
   ```

### 2. Build the Project

```bash
# Navigate to project directory
cd "Personal Expense Tracker"

# Compile and build
mvn clean compile

# Run tests
mvn test
```

### 3. Run the Application

**Option 1: Using Maven**
```bash
mvn javafx:run
```

**Option 2: Using Java directly**
```bash
# First, compile
mvn clean package

# Then run
java --module-path <path-to-javafx-sdk/lib> --add-modules javafx.controls,javafx.fxml -cp target/classes com.expensetracker.MainApp
```

**Option 3: Using IDE**
- Open the project in your IDE
- Run `MainApp.java` as a Java application

### 4. Default Login Credentials

- **Username**: `admin`
- **Password**: `admin123`

Or register a new user from the login screen.

## Project Structure

```
Personal Expense Tracker/
├── src/
│   ├── main/
│   │   ├── java/com/expensetracker/
│   │   │   ├── MainApp.java                    # JavaFX application entry point
│   │   │   ├── model/                          # Domain models
│   │   │   │   ├── Transaction.java            # Abstract base class
│   │   │   │   ├── Expense.java                # Expense subclass
│   │   │   │   ├── Income.java                 # Income subclass
│   │   │   │   ├── Category.java
│   │   │   │   └── User.java
│   │   │   ├── dao/                            # Data Access Object interfaces
│   │   │   │   ├── TransactionDAO.java
│   │   │   │   ├── CategoryDAO.java
│   │   │   │   └── UserDAO.java
│   │   │   ├── dao/impl/                       # DAO implementations
│   │   │   │   ├── TransactionDAOImpl.java
│   │   │   │   ├── CategoryDAOImpl.java
│   │   │   │   └── UserDAOImpl.java
│   │   │   ├── service/                        # Business logic
│   │   │   │   ├── AbstractService.java        # Abstract base class
│   │   │   │   ├── ReportGenerator.java        # Report generation
│   │   │   │   ├── CSVExporter.java
│   │   │   │   └── CSVImporter.java
│   │   │   ├── ui/                             # JavaFX controllers
│   │   │   │   ├── LoginController.java
│   │   │   │   └── MainController.java         # Main UI controller with multithreading
│   │   │   ├── util/                           # Utility classes
│   │   │   │   ├── DatabaseManager.java        # Connection pooling
│   │   │   │   └── Paginator.java              # Generic pagination utility
│   │   │   └── exceptions/                     # Custom exceptions
│   │   │       ├── DatabaseException.java
│   │   │       └── ValidationException.java
│   │   ├── resources/
│   │   │   ├── fxml/                           # FXML UI files
│   │   │   │   ├── Login.fxml
│   │   │   │   └── Main.fxml
│   │   │   └── sql/                            # SQL scripts
│   │   │       ├── schema.sql
│   │   │       └── sample_data.sql
│   └── test/
│       └── java/com/expensetracker/
│           ├── util/
│           │   └── PaginatorTest.java
│           ├── dao/impl/
│           │   └── TransactionDAOImplTest.java
│           └── service/
│               ├── ReportGeneratorTest.java
│               └── CSVExporterTest.java
├── docs/                                       # Documentation and presentation
├── pom.xml                                     # Maven configuration
└── README.md
```

## Rubric Mapping

This section maps each rubric requirement to specific code locations.

### 1. Object-Oriented Programming (OOP)

**Requirement**: Abstract Transaction class with Expense and Income subclasses, interfaces, abstract classes, polymorphism.

**Implementation**:
- **Abstract Class**: `src/main/java/com/expensetracker/model/Transaction.java` (lines 1-100)
  - Abstract methods: `getType()`, `getSignedAmount()`
- **Subclasses**: 
  - `src/main/java/com/expensetracker/model/Expense.java` (lines 1-35)
  - `src/main/java/com/expensetracker/model/Income.java` (lines 1-35)
- **Interfaces**: 
  - `src/main/java/com/expensetracker/dao/TransactionDAO.java` (lines 1-60)
  - `src/main/java/com/expensetracker/dao/CategoryDAO.java`
  - `src/main/java/com/expensetracker/dao/UserDAO.java`
- **Abstract Service Class**: `src/main/java/com/expensetracker/service/AbstractService.java` (lines 1-60)
- **Polymorphism**: `src/main/java/com/expensetracker/service/ReportGenerator.java` (lines 20-50)
  - Methods accept `List<Transaction>` and process both Expense and Income through polymorphism
  - Example: `calculateTotalIncome()` filters by `instanceof Income` and processes via Transaction interface

### 2. Collections & Generics

**Requirement**: Use List/Map/SortedMap, implement one generic utility class.

**Implementation**:
- **Generic Utility**: `src/main/java/com/expensetracker/util/Paginator.java` (entire file)
  - Generic class `Paginator<T>` for paginating any type of list
- **Collections Usage**:
  - `List<Transaction>`: Used throughout (e.g., `ReportGenerator.java` line 20)
  - `Map<String, BigDecimal>`: `ReportGenerator.generateCategoryBreakdown()` (line 80)
  - `SortedMap<YearMonth, MonthlySummary>`: `ReportGenerator.generateMonthlySummary()` (line 60)
  - `SortedMap<LocalDate, BigDecimal>`: `ReportGenerator.generateDailyTotals()` (line 100)
- **Synchronized Collections**: `MainController.java` line 50
  - `Collections.synchronizedList()` for thread-safe transaction cache

### 3. Multithreading & Synchronization

**Requirement**: JavaFX Task + ExecutorService for background tasks, progress bar for import, synchronization.

**Implementation**:
- **ExecutorService**: `src/main/java/com/expensetracker/ui/MainController.java` (line 45)
  - `ExecutorService executorService = Executors.newFixedThreadPool(4)`
- **JavaFX Task**: Multiple examples in `MainController.java`:
  - `handleAddTransaction()`: Task for database insert (line 120)
  - `handleImportCSV()`: Task with progress bar (line 280)
  - `importTransactionsWithProgress()`: Demonstrates 10k row import with progress (line 300)
- **Progress Bar**: `Main.fxml` line 45, bound to Task progress in `MainController.java` line 350
- **Synchronization**: 
  - `ReadWriteLock`: `MainController.java` line 46
  - `dataLock.readLock()` / `dataLock.writeLock()`: Used throughout for thread-safe cache access (lines 150, 200, 400)
  - Synchronized collection: `Collections.synchronizedList()` (line 50)

### 4. Database Classes & JDBC

**Requirement**: DatabaseManager, DAO interface and implementation, prepared statements, batch insert, transactions, try-with-resources, custom exceptions.

**Implementation**:
- **DatabaseManager**: `src/main/java/com/expensetracker/util/DatabaseManager.java` (entire file)
  - HikariCP connection pooling
  - Singleton pattern
- **DAO Interface**: `src/main/java/com/expensetracker/dao/TransactionDAO.java`
- **DAO Implementation**: `src/main/java/com/expensetracker/dao/impl/TransactionDAOImpl.java`
  - All SQL uses prepared statements (lines 50-200)
  - Batch insert: `batchInsert()` method (line 180) with transaction management
  - Transaction commit/rollback: Lines 190-210
  - Try-with-resources: Used throughout (e.g., line 55)
- **Custom Exceptions**: 
  - `src/main/java/com/expensetracker/exceptions/DatabaseException.java`
  - `src/main/java/com/expensetracker/exceptions/ValidationException.java`

### 5. Features

**Requirement**: Authentication, category CRUD, transaction CRUD, filters, monthly summary, charts, CSV export/import, DB backup.

**Implementation**:
- **Authentication**: `LoginController.java` (lines 30-60)
- **Category CRUD**: `CategoryDAO.java` and `CategoryDAOImpl.java`
- **Transaction CRUD**: `TransactionDAO.java` and `TransactionDAOImpl.java`
- **Filters**: `MainController.handleFilter()` (line 220) - date range and category
- **Monthly Summary**: `ReportGenerator.generateMonthlySummary()` (line 60)
- **Charts**: 
  - Pie Chart: `MainController.updateCharts()` (line 420)
  - Line Chart: `MainController.updateCharts()` (line 435)
- **CSV Export**: `CSVExporter.java`, `MainController.handleExportCSV()` (line 250)
- **CSV Import**: `CSVImporter.java`, `MainController.handleImportCSV()` (line 270)
- **Sample Data Loader**: `MainController.generateSampleData()` (line 360)

### 6. Code Quality

**Requirement**: Modular packages, comments, unit tests, meaningful commits, README.

**Implementation**:
- **Packages**: 
  - `model`, `dao`, `dao.impl`, `service`, `ui`, `util`, `exceptions`
- **Comments**: All classes have JavaDoc comments
- **Unit Tests**: 
  - `src/test/java/com/expensetracker/util/PaginatorTest.java`
  - `src/test/java/com/expensetracker/dao/impl/TransactionDAOImplTest.java`
  - `src/test/java/com/expensetracker/service/ReportGeneratorTest.java`
  - `src/test/java/com/expensetracker/service/CSVExporterTest.java`
- **README**: This file

### 7. Presentation

**Requirement**: 10-slide PPTX in /docs with screenshots, ER & class diagrams, run instructions, rubric mapping.

**Implementation**: See `/docs/presentation.pptx` (to be generated)

## Testing

Run all tests:
```bash
mvn test
```

Run specific test class:
```bash
mvn test -Dtest=PaginatorTest
```

**Note**: Some tests require a database connection. Tests marked with `@Disabled` can be enabled once the database is configured.

## Database Schema

The database consists of three main tables:

1. **users**: User authentication
2. **categories**: Transaction categories (EXPENSE/INCOME)
3. **transactions**: Financial transactions with foreign key to categories

See `src/main/resources/sql/schema.sql` for the complete schema.

## Troubleshooting

### Database Connection Issues
- Verify MySQL is running
- Check credentials in `DatabaseManager.java`
- Ensure database `expense_tracker` exists

### JavaFX Runtime Issues
- Ensure JavaFX dependencies are in classpath
- Use `mvn javafx:run` for automatic JavaFX setup

### Build Issues
- Clean and rebuild: `mvn clean install`
- Check Java version: `java -version` (should be 17+)

## License

This project is created for educational purposes as part of a university assignment.

## Author

- Ayushman Mishra
- Daksh Sharma
- Akshay Kumar


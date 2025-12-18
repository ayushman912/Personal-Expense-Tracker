# Personal Expense Tracker API Documentation

This document describes the REST API endpoints for the Personal Expense Tracker server.

## Base URL
`http://localhost:8080`

## Authentication

### Register
- **Endpoint:** `/api/auth/register`
- **Method:** `POST`
- **Content-Type:** `application/json`
- **Request Body:**
  ```json
  {
    "username": "newuser",
    "email": "newuser@example.com",
    "password": "securePass123"
  }
  ```
- **Validation:**
  - `username`: Required, 3-30 characters, alphanumeric and underscores only
  - `email`: Optional, validated if provided
  - `password`: Required, minimum 6 characters
- **Response:**
  - `201 Created`: User registered successfully.
    ```json
    {
      "message": "User created successfully",
      "userId": 5
    }
    ```
  - `400 Bad Request`: Validation error (invalid input format).
    ```json
    {
      "error": "Username must be 3-30 characters, containing only letters, numbers, and underscores"
    }
    ```
  - `409 Conflict`: Username already exists.
    ```json
    {
      "error": "Username already exists"
    }
    ```
- **Notes:** 
  - Passwords are hashed using BCrypt before storage.
  - Registration does not auto-login; call `/api/auth/login` after registration.

### Login
- **Endpoint:** `/api/auth/login`
- **Method:** `POST`
- **Content-Type:** `application/json`
- **Request Body:**
  ```json
  {
    "username": "admin",
    "password": "password"
  }
  ```
- **Response:**
  - `200 OK`: Returns JSON with token and user details.
  - `401 Unauthorized`: Invalid credentials.

## Transactions

### Get All Transactions
- **Endpoint:** `/api/transactions`
- **Method:** `GET`
- **Query Parameters:**
  - `startDate` (optional): Filter by start date (YYYY-MM-DD).
  - `endDate` (optional): Filter by end date (YYYY-MM-DD).
  - `categoryId` (optional): Filter by category ID.
- **Response:**
  - `200 OK`: List of transactions.

### Add Transaction
- **Endpoint:** `/api/transactions`
- **Method:** `POST`
- **Content-Type:** `application/json`
- **Request Body:**
  ```json
  {
    "amount": 100.00,
    "description": "Groceries",
    "date": "2023-10-27",
    "categoryId": 1,
    "type": "EXPENSE"
  }
  ```
- **Response:**
  - `201 Created`: Returns the created transaction with ID.

### Update Transaction
- **Endpoint:** `/api/transactions/{id}`
- **Method:** `PUT`
- **Content-Type:** `application/json`
- **Request Body:** Same as Add Transaction.
- **Response:**
  - `200 OK`: Returns updated transaction.

### Delete Transaction
- **Endpoint:** `/api/transactions/{id}`
- **Method:** `DELETE`
- **Response:**
  - `204 No Content`: Successful deletion.

## Categories

### Get All Categories
- **Endpoint:** `/api/categories`
- **Method:** `GET`
- **Response:**
  - `200 OK`: List of categories.

### Add Category
- **Endpoint:** `/api/categories`
- **Method:** `POST`
- **Content-Type:** `application/json`
- **Request Body:**
  ```json
  {
    "name": "New Category",
    "type": "EXPENSE",
    "description": "Optional description"
  }
  ```
- **Response:**
  - `201 Created`: Returns created category.

## Backup

### Trigger Backup
- **Endpoint:** `/api/backup`
- **Method:** `POST`
- **Response:**
  - `200 OK`: Backup triggered successfully.

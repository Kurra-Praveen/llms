# Multi-Tenant Loan Management Platform

## Overview
Enterprise-grade multi-tenant SaaS loan management platform for small-scale lenders, built with Spring Boot 3.x and Java 21.

### Project Status

| Phase | Status | Details |
|-------|--------|---------|
| Backend API | **Complete** | All endpoints implemented and tested |
| Unit Tests | **Complete** | 65 tests passing |
| Integration Tests | **Complete** | 50 tests passing |
| Documentation | **Complete** | Full API and architecture docs |
| Frontend | *Not Started* | Planned: React + TypeScript |

### Quick Links
- [API Documentation](docs/API_DOCUMENTATION.md) - Complete API reference
- [Database Schema](docs/DATABASE_SCHEMA.md) - Database tables and relationships
- [Architecture](docs/ARCHITECTURE.md) - System design and patterns
- [Project Context](docs/PROJECT_CONTEXT.md) - Comprehensive project summary

## Technology Stack
- **Java 21** (LTS)
- **Spring Boot 3.2.x**
- **PostgreSQL 15** with Flyway migrations
- **Redis 7** for caching
- **Docker & Docker Compose** for containerization
- **Spring Security** with JWT authentication
- **Spring Data JPA** with Hibernate tenant filtering
- **MapStruct** for DTO mapping
- **Springdoc OpenAPI** for API documentation

## Features
- Multi-tenant data isolation with tenant-aware queries
- Multiple loan types (Flat, Reducing Balance, Simple, Interest-Only, Bullet)
- Automated EMI/schedule calculation
- Payment waterfall allocation (Penalty → Interest → Principal)
- Double-entry ledger accounting
- Overdue/penalty management
- Notification system
- Comprehensive audit trails

## Getting Started

### Prerequisites
- Java 21
- Maven 3.9+
- Docker & Docker Compose

### Quick Start with Docker (Recommended)

1. **Start PostgreSQL and Redis containers:**
```bash
cd loan-platform
./docker.sh start
```

2. **Run the application:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

3. **Access the API:**
- API Base URL: http://localhost:4044/api
- Swagger UI: http://localhost:4044/api/swagger-ui.html

### Docker Commands

```bash
# Start only PostgreSQL and Redis
./docker.sh start

# Start with management tools (pgAdmin, Redis Commander)
./docker.sh start-all

# Stop all containers
./docker.sh stop

# View logs
./docker.sh logs
./docker.sh logs postgres

# Connect to PostgreSQL CLI
./docker.sh psql

# Connect to Redis CLI
./docker.sh redis-cli

# Remove all containers and data
./docker.sh clean

# Check container status
./docker.sh status
```

### Management Tools (Optional)

Start with `./docker.sh start-all` to access:

| Tool | URL | Credentials |
|------|-----|-------------|
| pgAdmin | http://localhost:5050 | admin@loanplatform.com / admin |
| Redis Commander | http://localhost:8081 | - |

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | loan_platform | Database name |
| `DB_USERNAME` | postgres | Database username |
| `DB_PASSWORD` | postgres | Database password |
| `REDIS_HOST` | localhost | Redis host |
| `REDIS_PORT` | 6379 | Redis port |
| `JWT_SECRET` | (default) | JWT signing secret |

### Running Without Docker

If you prefer to install PostgreSQL and Redis locally:

```bash
# Create database
createdb loan_platform

# Run application
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## PostgreSQL Docker Setup Guide

This section provides detailed instructions for setting up and connecting to PostgreSQL using Docker.

### Option 1: Using Docker Compose (Recommended)

#### Step 1: Start PostgreSQL Container

```bash
cd loan-platform

# Start PostgreSQL and Redis
docker-compose up -d postgres redis

# Or use the helper script
./docker.sh start
```

#### Step 2: Verify Container is Running

```bash
docker ps

# Expected output:
# CONTAINER ID   IMAGE                PORTS                    NAMES
# xxxx           postgres:15-alpine   0.0.0.0:5432->5432/tcp   loan-platform-postgres
# xxxx           redis:7-alpine       0.0.0.0:6379->6379/tcp   loan-platform-redis
```

#### Step 3: Connect to PostgreSQL

**Method A: Using docker exec (Interactive Shell)**
```bash
# Connect to psql inside the container
docker exec -it loan-platform-postgres psql -U postgres -d loan_platform

# Or use the helper script
./docker.sh psql
```

**Method B: Using psql from Host Machine**
```bash
# If you have psql installed locally
psql -h localhost -p 5432 -U postgres -d loan_platform
# Password: postgres
```

**Method C: Using Connection String**
```
postgresql://postgres:postgres@localhost:5432/loan_platform
```

### Option 2: Using Standalone Docker Container

If you prefer not to use docker-compose:

```bash
# Pull the PostgreSQL image
docker pull postgres:15-alpine

# Create and run the container
docker run -d \
  --name loan-platform-postgres \
  -e POSTGRES_DB=loan_platform \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -v loan_platform_data:/var/lib/postgresql/data \
  postgres:15-alpine

# Verify it's running
docker ps

# Connect to the database
docker exec -it loan-platform-postgres psql -U postgres -d loan_platform
```

### Connection Details

| Parameter | Value |
|-----------|-------|
| **Host** | `localhost` (from host machine) or `postgres` (from other containers) |
| **Port** | `5432` |
| **Database** | `loan_platform` |
| **Username** | `postgres` |
| **Password** | `postgres` |
| **JDBC URL** | `jdbc:postgresql://localhost:5432/loan_platform` |
| **Connection String** | `postgresql://postgres:postgres@localhost:5432/loan_platform` |

### Using pgAdmin (GUI Tool)

#### Step 1: Start pgAdmin

```bash
# Start with management tools
docker-compose --profile tools up -d

# Or use the helper script
./docker.sh start-all
```

#### Step 2: Access pgAdmin

1. Open browser: http://localhost:5050
2. Login credentials:
   - Email: `admin@loanplatform.com`
   - Password: `admin`

#### Step 3: Add Server Connection in pgAdmin

1. Right-click "Servers" → "Register" → "Server"
2. **General Tab:**
   - Name: `Loan Platform`
3. **Connection Tab:**
   - Host: `postgres` (use container name, not localhost)
   - Port: `5432`
   - Database: `loan_platform`
   - Username: `postgres`
   - Password: `postgres`
   - Save password: ✓
4. Click "Save"

### Using DBeaver or Other GUI Tools

Configure connection with these settings:
- **Host:** `localhost`
- **Port:** `5432`
- **Database:** `loan_platform`
- **Username:** `postgres`
- **Password:** `postgres`

### Common Docker Commands

```bash
# View PostgreSQL logs
docker logs loan-platform-postgres

# Follow logs in real-time
docker logs -f loan-platform-postgres

# Stop PostgreSQL container
docker stop loan-platform-postgres

# Start stopped container
docker start loan-platform-postgres

# Remove container (data preserved in volume)
docker rm loan-platform-postgres

# Remove container AND data
docker rm loan-platform-postgres
docker volume rm loan-platform_postgres_data

# Restart container
docker restart loan-platform-postgres

# Execute SQL file
docker exec -i loan-platform-postgres psql -U postgres -d loan_platform < script.sql

# Backup database
docker exec loan-platform-postgres pg_dump -U postgres loan_platform > backup.sql

# Restore database
docker exec -i loan-platform-postgres psql -U postgres -d loan_platform < backup.sql
```

### Troubleshooting

#### Port Already in Use
```bash
# Check what's using port 5432
lsof -i :5432
# or
netstat -tulpn | grep 5432

# Stop local PostgreSQL if running
sudo systemctl stop postgresql

# Or change the port in docker-compose.yml
ports:
  - "5433:5432"  # Use 5433 on host
```

#### Container Won't Start
```bash
# Check container logs
docker logs loan-platform-postgres

# Check if volume has permissions issues
docker volume inspect loan-platform_postgres_data

# Remove and recreate
docker-compose down -v
docker-compose up -d
```

#### Connection Refused
```bash
# Ensure container is running
docker ps | grep postgres

# Check container health
docker inspect loan-platform-postgres | grep -A 10 "Health"

# Wait for PostgreSQL to be ready
docker exec loan-platform-postgres pg_isready -U postgres
```

#### Reset Database (Fresh Start)
```bash
# Stop and remove everything
./docker.sh clean

# Or manually:
docker-compose down -v
docker volume rm loan-platform_postgres_data

# Start fresh
docker-compose up -d
```

### Environment Variables for Application

When running the Spring Boot application, you can override database settings:

```bash
# Using environment variables
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=loan_platform
export DB_USERNAME=postgres
export DB_PASSWORD=postgres

mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or inline
DB_HOST=localhost DB_PORT=5432 mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker Network (For Microservices)

If running multiple services that need to communicate:

```bash
# Services on the same Docker network can use container names
# Example: from another container, connect to:
# Host: postgres (not localhost)
# Port: 5432

# The network is defined in docker-compose.yml as: loan-platform-network
```

---

## Project Structure
```
com.loanplatform
├── auth/           # Authentication & authorization
├── tenant/         # Tenant management
├── borrower/       # Borrower management
├── loan/           # Loan management & interest engine
├── payment/        # Payment processing & allocation
├── ledger/         # Double-entry accounting
├── collection/     # Collections & dunning
├── notification/   # Notification system
├── scheduler/      # Scheduled jobs
├── audit/          # Audit logging
└── common/         # Shared utilities & config
```

## API Endpoints

### Authentication
- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/refresh` - Refresh token
- `POST /api/v1/auth/logout` - Logout
- `GET /api/v1/auth/me` - Current user

### Tenants (Super Admin)
- `POST /api/v1/tenants` - Create tenant
- `GET /api/v1/tenants` - List tenants
- `GET /api/v1/tenants/{id}` - Get tenant

### Borrowers
- `POST /api/v1/borrowers` - Create borrower
- `GET /api/v1/borrowers` - List borrowers
- `GET /api/v1/borrowers/{id}` - Get borrower
- `PUT /api/v1/borrowers/{id}` - Update borrower

### Loans
- `POST /api/v1/loans` - Create loan
- `POST /api/v1/loans/{id}/disburse` - Disburse loan
- `GET /api/v1/loans` - List loans
- `GET /api/v1/loans/{id}` - Get loan with schedule
- `GET /api/v1/loans/{id}/schedule` - Get repayment schedule

### Payments
- `POST /api/v1/payments` - Record payment
- `GET /api/v1/payments` - List payments
- `GET /api/v1/payments/loan/{loanId}` - Payments for loan
- `POST /api/v1/payments/{id}/reverse` - Reverse payment

## Docker Compose Services

| Service | Port | Description |
|---------|------|-------------|
| postgres | 5432 | PostgreSQL 15 database |
| redis | 6379 | Redis 7 cache |
| pgadmin | 5050 | Database management UI |
| redis-commander | 8081 | Redis management UI |

## License
Proprietary - All rights reserved

---

## Test Summary

Run all tests:
```bash
mvn test
```

Current test results:
- **65 Unit Tests** - All passing
- **50 Integration Tests** - All passing (1 skipped)

### Test Coverage by Module

| Module | Unit Tests | Integration Tests |
|--------|------------|-------------------|
| Auth | 18 | 7 |
| Tenant | 9 | 12 |
| Borrower | 10 | 10 |
| Loan | 20 | 11 |
| Payment | 8 | 10 |

---

## Documentation

Comprehensive documentation is available in the `docs/` directory:

| Document | Description |
|----------|-------------|
| [API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md) | Complete REST API reference with request/response examples |
| [DATABASE_SCHEMA.md](docs/DATABASE_SCHEMA.md) | Database tables, columns, indexes, and relationships |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | System architecture, design patterns, and flows |
| [PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md) | Project summary for context continuity |

---

*Backend Phase 1 Complete - Ready for Frontend Development*

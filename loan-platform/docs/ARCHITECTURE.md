# Architecture Documentation

System architecture and design patterns for the Multi-Tenant Loan Management Platform.

## Table of Contents

1. [System Overview](#system-overview)
2. [Technology Stack](#technology-stack)
3. [Module Structure](#module-structure)
4. [Multi-Tenancy Architecture](#multi-tenancy-architecture)
5. [Authentication & Authorization](#authentication--authorization)
6. [Core Business Flows](#core-business-flows)
7. [Interest Calculation Engine](#interest-calculation-engine)
8. [Payment Allocation Engine](#payment-allocation-engine)
9. [API Design Patterns](#api-design-patterns)
10. [Testing Strategy](#testing-strategy)

---

## System Overview

The Multi-Tenant Loan Management Platform is a B2B SaaS application designed for microfinance institutions to manage their lending operations. It supports multiple tenants (lenders) with complete data isolation.

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Frontend (React)                            │
│                        (To be implemented - Phase 2)                     │
└─────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           API Gateway / Load Balancer                    │
└─────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Spring Boot Application                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ │
│  │    Auth     │  │   Tenant    │  │  Borrower   │  │      Loan       │ │
│  │   Module    │  │   Module    │  │   Module    │  │     Module      │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────┘ │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ │
│  │   Payment   │  │   Ledger    │  │ Collection  │  │  Notification   │ │
│  │   Module    │  │   Module    │  │   Module    │  │     Module      │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────┘ │
│  ┌─────────────┐  ┌─────────────┐                                       │
│  │   Audit     │  │  Scheduler  │                                       │
│  │   Module    │  │   Module    │                                       │
│  └─────────────┘  └─────────────┘                                       │
└─────────────────────────────────────────────────────────────────────────┘
         │                    │                         │
         ▼                    ▼                         ▼
┌──────────────────┐  ┌─────────────────┐      ┌───────────────┐
│   PostgreSQL     │  │     Redis       │      │   External    │
│   (Primary DB)   │  │ (Cache/Session) │      │   Services    │
└──────────────────┘  └─────────────────┘      │  - SMS/Email  │
                                               │  - Payments   │
                                               └───────────────┘
```

---

## Technology Stack

### Backend
| Component | Technology | Version |
|-----------|------------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.2.x |
| Security | Spring Security + JWT | 3.2.x |
| ORM | Hibernate/JPA | 6.x |
| Database | PostgreSQL | 15+ |
| Cache | Redis | 7.x |
| Build Tool | Maven | 3.9.x |

### Key Libraries
- **Lombok** - Boilerplate reduction
- **MapStruct** (optional) - Object mapping
- **SpringDoc OpenAPI** - API documentation
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **H2** - In-memory testing database

---

## Module Structure

### Package Organization

```
com.loanplatform
├── common/                      # Shared components
│   ├── config/                  # Configuration classes
│   │   ├── TenantContext.java   # ThreadLocal tenant storage
│   │   ├── TenantFilter.java    # HTTP filter for tenant extraction
│   │   └── OpenApiConfig.java   # Swagger configuration
│   ├── dto/                     # Common DTOs
│   │   ├── ApiResponse.java     # Standard response wrapper
│   │   └── PagedResponse.java   # Pagination wrapper
│   ├── entity/                  # Base entity classes
│   │   ├── BaseEntity.java      # ID, timestamps, version
│   │   ├── AuditableEntity.java # + created_by, updated_by
│   │   └── TenantAwareEntity.java # + tenant_id
│   ├── exception/               # Exception handling
│   │   ├── BusinessException.java
│   │   ├── ResourceNotFoundException.java
│   │   └── GlobalExceptionHandler.java
│   └── util/                    # Utility classes
│
├── auth/                        # Authentication module
│   ├── controller/              # REST endpoints
│   ├── dto/                     # Request/Response DTOs
│   ├── entity/                  # User, Role, RefreshToken
│   ├── repository/              # Data access
│   ├── service/                 # Business logic
│   │   ├── AuthService.java
│   │   └── JwtService.java
│   └── security/                # Security config
│       ├── JwtAuthenticationFilter.java
│       └── SecurityConfig.java
│
├── tenant/                      # Tenant management
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── mapper/
│
├── borrower/                    # Borrower management
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── mapper/
│
├── loan/                        # Loan management
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   ├── mapper/
│   └── engine/                  # Calculation engines
│       ├── InterestCalculationEngine.java
│       ├── ScheduleGenerationRequest.java
│       └── ScheduleEntry.java
│
├── payment/                     # Payment processing
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   ├── mapper/
│   └── engine/
│       └── PaymentAllocationEngine.java
│
├── ledger/                      # Accounting/Ledger
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── dto/
│
├── collection/                  # Collections management
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── controller/
│
├── notification/                # Notifications
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── controller/
│
├── scheduler/                   # Scheduled jobs
│   └── ScheduledTasks.java
│
└── audit/                       # Audit logging
    ├── entity/
    └── repository/
```

### Layer Responsibilities

```
┌─────────────────────────────────────────────────────────────┐
│                      Controller Layer                        │
│  - HTTP request handling                                     │
│  - Request validation (@Valid)                               │
│  - Response formatting (ApiResponse)                         │
│  - Authorization checks (@PreAuthorize)                      │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                       Service Layer                          │
│  - Business logic                                            │
│  - Transaction management (@Transactional)                   │
│  - Cross-entity operations                                   │
│  - Event publishing                                          │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     Repository Layer                         │
│  - Data access (JpaRepository)                               │
│  - Custom queries (@Query)                                   │
│  - Tenant filtering                                          │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      Entity Layer                            │
│  - Domain models                                             │
│  - JPA annotations                                           │
│  - Business methods                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## Multi-Tenancy Architecture

### Row-Level Isolation Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP Request                              │
│              Authorization: Bearer <JWT>                     │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   TenantFilter.java                          │
│  1. Extract JWT from header                                  │
│  2. Parse tenant_id claim                                    │
│  3. Set TenantContext.setCurrentTenant(tenantId)            │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   TenantContext.java                         │
│  ThreadLocal<UUID> storage for current tenant                │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   TenantAspect.java                          │
│  AOP aspect that enables Hibernate filter before queries     │
│  session.enableFilter("tenantFilter")                        │
│         .setParameter("tenantId", TenantContext.get())       │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    Database Query                            │
│  SELECT * FROM borrowers WHERE ... AND tenant_id = ?         │
└─────────────────────────────────────────────────────────────┘
```

### TenantContext Implementation

```java
public class TenantContext {
    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    public static UUID getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentTenant(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID requireTenant() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
```

### Hibernate Filter Definition

```java
// On package-info.java
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = UUID.class)
)
package com.loanplatform;

// On each tenant-scoped entity
@Entity
@Table(name = "borrowers")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Borrower extends TenantAwareEntity { }
```

---

## Authentication & Authorization

### JWT Token Flow

```
┌──────────┐                  ┌──────────────┐              ┌──────────┐
│  Client  │                  │  Auth API    │              │ Database │
└────┬─────┘                  └──────┬───────┘              └────┬─────┘
     │                               │                           │
     │  POST /v1/auth/login          │                           │
     │  {email, password}            │                           │
     │──────────────────────────────>│                           │
     │                               │  Validate credentials     │
     │                               │──────────────────────────>│
     │                               │<──────────────────────────│
     │                               │                           │
     │                               │  Generate tokens          │
     │                               │  - Access (15 min)        │
     │                               │  - Refresh (7 days)       │
     │                               │                           │
     │  {accessToken, refreshToken}  │  Save refresh token       │
     │<──────────────────────────────│──────────────────────────>│
     │                               │                           │
     │  GET /v1/borrowers            │                           │
     │  Authorization: Bearer <jwt>  │                           │
     │──────────────────────────────>│                           │
     │                               │  Validate JWT             │
     │                               │  Extract claims           │
     │                               │  Set TenantContext        │
     │                               │  Check permissions        │
     │                               │                           │
     │  {data: [...]}                │                           │
     │<──────────────────────────────│                           │
```

### JWT Token Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user-uuid",
    "email": "user@example.com",
    "role": "LENDER_ADMIN",
    "tenantId": "tenant-uuid",
    "iat": 1705312800,
    "exp": 1705313700
  }
}
```

### Role-Based Access Control

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              SUPER_ADMIN                                 │
│  - Manage all tenants                                                    │
│  - Create/update/suspend tenants                                         │
│  - View platform-wide analytics                                          │
│  - No tenant_id (cross-tenant access)                                    │
└─────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                              LENDER_ADMIN                                │
│  - Full access within tenant                                             │
│  - Manage users                                                          │
│  - Configure settings                                                    │
│  - Approve loans                                                         │
│  - Reverse payments                                                      │
└─────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                              LENDER_STAFF                                │
│  - Manage borrowers                                                      │
│  - Create loan applications                                              │
│  - Record payments                                                       │
│  - View reports                                                          │
└─────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                                BORROWER                                  │
│  - View own loans                                                        │
│  - View payment history                                                  │
│  - Download statements                                                   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Security Configuration

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/v1/auth/login", "/v1/auth/refresh").permitAll()
            .requestMatchers("/v1/tenants/**").hasRole("SUPER_ADMIN")
            .requestMatchers(HttpMethod.POST, "/v1/payments/*/reverse")
                .hasRole("LENDER_ADMIN")
            .anyRequest().authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

---

## Core Business Flows

### 1. Loan Lifecycle

```
┌────────┐    ┌──────────────────┐    ┌──────────┐    ┌────────┐    ┌────────┐
│ DRAFT  │───>│ PENDING_APPROVAL │───>│ APPROVED │───>│ ACTIVE │───>│ CLOSED │
└────────┘    └──────────────────┘    └──────────┘    └────────┘    └────────┘
     │                │                     │              │
     │                │                     │              └──> WRITTEN_OFF
     │                │                     │
     │                └──> REJECTED         └──> CANCELLED
     │
     └──> CANCELLED
```

**State Transitions:**
- `DRAFT` → `PENDING_APPROVAL`: When loan requires approval
- `DRAFT` → `ACTIVE`: Direct disbursement (no approval needed)
- `PENDING_APPROVAL` → `APPROVED`: Approver approves
- `PENDING_APPROVAL` → `REJECTED`: Approver rejects
- `APPROVED` → `ACTIVE`: Disbursement
- `ACTIVE` → `CLOSED`: Full repayment
- `ACTIVE` → `WRITTEN_OFF`: Bad debt write-off

### 2. Loan Creation Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        POST /v1/loans                                    │
│  {borrowerId, principalAmount, interestRate, interestType, tenure...}   │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       LoanService.createLoan()                           │
│  1. Validate borrower exists and is ACTIVE                               │
│  2. Check tenant limits (maxLoans)                                       │
│  3. Generate loan number                                                 │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                   InterestCalculationEngine                              │
│  1. Calculate EMI based on interest type                                 │
│  2. Calculate total interest                                             │
│  3. Calculate total payable                                              │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Save Loan                                        │
│  Status: DRAFT (or PENDING_APPROVAL if configured)                       │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3. Loan Disbursement Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                   POST /v1/loans/{id}/disburse                           │
│                 {disbursementDate, firstPaymentDate}                     │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    LoanService.disburseLoan()                            │
│  1. Validate loan status is APPROVED or DRAFT (no approval needed)       │
│  2. Set disbursement date                                                │
│  3. Set outstanding = principal                                          │
│  4. Update status to ACTIVE                                              │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                   InterestCalculationEngine                              │
│  Generate repayment schedule based on:                                   │
│  - Interest type (FLAT, REDUCING_BALANCE, etc.)                          │
│  - Repayment frequency (MONTHLY, WEEKLY, etc.)                           │
│  - Tenure months                                                         │
│  - Grace period days                                                     │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 Save RepaymentSchedule entries                           │
│  N installments with due dates, principal/interest components            │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4. Payment Processing Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      POST /v1/payments                                   │
│           {loanId, amount, paymentDate, paymentMethod}                   │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                   Check idempotency key                                  │
│  If exists, return existing payment                                      │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                   Validate loan is ACTIVE                                │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    Create Payment record                                 │
│  Generate payment number, receipt number                                 │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                  PaymentAllocationEngine                                 │
│  Allocate payment to unpaid schedules:                                   │
│  1. Get unpaid schedules (oldest first)                                  │
│  2. For each schedule:                                                   │
│     a. Allocate to penalty first                                         │
│     b. Allocate to interest second                                       │
│     c. Allocate to principal last                                        │
│  3. Track excess amount                                                  │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              Update loan outstandings                                    │
│  - Reduce outstanding principal/interest/penalty                         │
│  - Increase paid amounts                                                 │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              Check if loan is fully paid                                 │
│  If totalOutstanding <= 0, close loan                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Interest Calculation Engine

### Supported Interest Types

#### 1. FLAT Rate
- Interest calculated on original principal for entire tenure
- Same interest amount each installment
- Formula: `Total Interest = Principal × (Rate/100) × (Months/12)`

#### 2. REDUCING_BALANCE (EMI)
- Interest calculated on remaining principal
- Equal installment amount (EMI)
- Formula: `EMI = P × r × (1+r)^n / ((1+r)^n - 1)`

#### 3. SIMPLE Interest
- Interest = Principal × Rate × Time
- Equal principal + interest payments

#### 4. INTEREST_ONLY
- Pay only interest during tenure
- Principal repaid at maturity

#### 5. BULLET
- Single payment at maturity
- Principal + all interest paid at end

### Schedule Generation

```java
public List<ScheduleEntry> generateSchedule(ScheduleGenerationRequest request) {
    return switch (request.getInterestType()) {
        case FLAT -> generateFlatSchedule(request);
        case REDUCING_BALANCE -> generateReducingBalanceSchedule(request);
        case SIMPLE -> generateSimpleInterestSchedule(request);
        case INTEREST_ONLY -> generateInterestOnlySchedule(request);
        case BULLET -> generateBulletSchedule(request);
    };
}
```

---

## Payment Allocation Engine

### Allocation Waterfall

```
Payment Amount
      │
      ▼
┌─────────────────────────┐
│  1. Outstanding Penalty │  ← Allocate first
└──────────┬──────────────┘
           │ Remaining
           ▼
┌─────────────────────────┐
│  2. Outstanding Interest│  ← Allocate second
└──────────┬──────────────┘
           │ Remaining
           ▼
┌─────────────────────────┐
│  3. Outstanding Principal│ ← Allocate third
└──────────┬──────────────┘
           │ Remaining
           ▼
┌─────────────────────────┐
│  4. Excess Amount       │  ← Applied to future or held
└─────────────────────────┘
```

### Schedule Processing Order

1. Get all unpaid schedules ordered by installment number (FIFO)
2. For each schedule, apply allocation waterfall
3. Update schedule status (PARTIAL or PAID)
4. Continue until payment exhausted or all schedules paid

---

## API Design Patterns

### Standard Response Wrapper

```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    private List<String> errors;
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data) { }
    public static <T> ApiResponse<T> success(T data, String message) { }
    public static ApiResponse<?> error(String message, String errorCode) { }
}
```

### Pagination Response

```java
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}
```

### API Versioning

- URL-based versioning: `/v1/...`
- All endpoints prefixed with version

### Error Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage(), "RESOURCE_NOT_FOUND"));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }
}
```

---

## Testing Strategy

### Test Pyramid

```
                    ┌───────────┐
                    │    E2E    │  ← Manual/Playwright (Future)
                    └─────┬─────┘
                          │
              ┌───────────┴───────────┐
              │    Integration Tests   │  ← MockMvc, H2 Database
              │    (50 tests)          │
              └───────────┬───────────┘
                          │
    ┌─────────────────────┴─────────────────────┐
    │              Unit Tests                    │  ← Mockito, JUnit 5
    │              (65 tests)                    │
    └────────────────────────────────────────────┘
```

### Unit Tests
- Service layer logic
- Calculation engines
- Mappers
- Mocked dependencies

### Integration Tests
- Controller endpoints
- Full request/response flow
- H2 in-memory database
- Mocked Redis

### Test Configuration

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BaseIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    protected Tenant testTenant;
    protected User testUser;

    @BeforeEach
    void setUp() {
        // Create test tenant and user
    }

    protected String getAuthHeader() {
        return "Bearer " + jwtService.generateAccessToken(testUser);
    }
}
```

---

## Configuration

### Application Properties

```yaml
spring:
  application:
    name: loan-platform
  datasource:
    url: jdbc:postgresql://localhost:5432/loandb
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

jwt:
  secret: ${JWT_SECRET}
  access-token-expiry: 3600000    # 1 hour
  refresh-token-expiry: 604800000 # 7 days

server:
  port: 4044
  servlet:
    context-path: /api
```

### Environment-Specific

| Profile | Database | Cache | Purpose |
|---------|----------|-------|---------|
| `dev` | PostgreSQL | Redis | Development |
| `test` | H2 (memory) | Mocked | Testing |
| `prod` | PostgreSQL | Redis | Production |

# API Documentation

Complete API reference for the Multi-Tenant Loan Management Platform.

## Base URL

```
http://localhost:4044/api
```

## Authentication

All API endpoints (except login) require JWT authentication.

### Headers

```
Authorization: Bearer <access_token>
Content-Type: application/json
```

## Response Format

All responses follow a standard format:

### Success Response

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Error Response

```json
{
  "success": false,
  "message": "Error description",
  "errorCode": "ERROR_CODE",
  "errors": ["Validation error 1", "Validation error 2"],
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Paged Response

```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false
  }
}
```

---

## Authentication Endpoints

### POST /v1/auth/login

Login with email and password.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "uuid",
      "tenantId": "uuid",
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "role": "LENDER_ADMIN"
    }
  }
}
```

### POST /v1/auth/refresh

Refresh access token using refresh token.

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

### POST /v1/auth/logout

Logout and revoke all refresh tokens.

**Response:**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

### GET /v1/auth/me

Get current user profile.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "tenantId": "uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "LENDER_ADMIN",
    "status": "ACTIVE",
    "lastLoginAt": "2024-01-15T10:30:00Z"
  }
}
```

### POST /v1/auth/users

Create a new user (SUPER_ADMIN or LENDER_ADMIN only).

**Request:**
```json
{
  "tenantId": "uuid",
  "email": "newuser@example.com",
  "password": "Password123!",
  "firstName": "Jane",
  "lastName": "Doe",
  "phone": "1234567890",
  "role": "LENDER_STAFF"
}
```

---

## Tenant Endpoints

### POST /v1/tenants

Create a new tenant (SUPER_ADMIN only).

**Request:**
```json
{
  "businessName": "ABC Microfinance",
  "businessCode": "ABC-MFI",
  "contactEmail": "contact@abcmfi.com",
  "contactPhone": "1234567890",
  "address": "123 Main Street",
  "subscriptionPlan": "PREMIUM",
  "maxBorrowers": 1000,
  "maxLoans": 5000,
  "adminEmail": "admin@abcmfi.com",
  "adminPassword": "AdminPass123!",
  "adminFirstName": "Admin",
  "adminLastName": "User"
}
```

### GET /v1/tenants

List all tenants with pagination.

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 20)
- `sortBy` (default: createdAt)
- `sortDir` (default: desc)

### GET /v1/tenants/{id}

Get tenant by ID.

### GET /v1/tenants/code/{code}

Get tenant by business code.

### PUT /v1/tenants/{id}

Update tenant.

**Request:**
```json
{
  "businessName": "Updated Name",
  "contactEmail": "updated@example.com",
  "maxBorrowers": 2000
}
```

### POST /v1/tenants/{id}/activate

Activate a suspended tenant.

### POST /v1/tenants/{id}/suspend

Suspend a tenant.

### POST /v1/tenants/{id}/deactivate

Deactivate a tenant.

### GET /v1/tenants/search

Search tenants.

**Query Parameters:**
- `q` - Search term
- `page`, `size`

---

## Borrower Endpoints

### POST /v1/borrowers

Create a new borrower.

**Request:**
```json
{
  "fullName": "John Doe",
  "dateOfBirth": "1990-05-15",
  "gender": "MALE",
  "phone": "9876543210",
  "alternatePhone": "9876543211",
  "email": "john.doe@example.com",
  "idType": "NATIONAL_ID",
  "idNumber": "ABC123456",
  "addressLine1": "123 Main Street",
  "addressLine2": "Apt 4B",
  "city": "New York",
  "state": "NY",
  "postalCode": "10001",
  "country": "USA",
  "occupation": "Engineer",
  "employerName": "Tech Corp",
  "monthlyIncome": 50000.00,
  "notes": "VIP customer"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "borrowerCode": "BRW-ABCD-000001",
    "fullName": "John Doe",
    "phone": "9876543210",
    "status": "ACTIVE",
    "riskScore": 70,
    "riskBand": "MEDIUM",
    "totalLoans": 0,
    "activeLoans": 0,
    "totalOutstanding": 0
  }
}
```

### GET /v1/borrowers

List borrowers with pagination.

**Query Parameters:**
- `page`, `size`, `sortBy`, `sortDir`

### GET /v1/borrowers/{id}

Get borrower by ID.

### GET /v1/borrowers/code/{code}

Get borrower by code.

### PUT /v1/borrowers/{id}

Update borrower.

### POST /v1/borrowers/{id}/block

Block a borrower.

### POST /v1/borrowers/{id}/activate

Activate a blocked borrower.

### DELETE /v1/borrowers/{id}

Soft delete a borrower.

### GET /v1/borrowers/search

Search borrowers by name, phone, or code.

### GET /v1/borrowers/status/{status}

Get borrowers by status (ACTIVE, BLOCKED, BLACKLISTED).

---

## Loan Endpoints

### POST /v1/loans

Create a new loan.

**Request:**
```json
{
  "borrowerId": "uuid",
  "principalAmount": 100000.00,
  "interestRate": 12.00,
  "interestType": "REDUCING_BALANCE",
  "tenureMonths": 12,
  "repaymentFrequency": "MONTHLY",
  "processingFee": 1000.00,
  "gracePeriodDays": 5,
  "hasCollateral": true,
  "collateralType": "VEHICLE",
  "collateralValue": 200000.00,
  "collateralDescription": "2022 Toyota Camry",
  "notes": "First loan for this customer"
}
```

**Interest Types:**
- `FLAT` - Simple flat rate interest
- `REDUCING_BALANCE` - EMI-based reducing balance
- `SIMPLE` - Simple interest
- `BULLET` - Principal at end with periodic interest
- `INTEREST_ONLY` - Interest-only with principal at end

**Repayment Frequencies:**
- `DAILY`
- `WEEKLY`
- `BI_WEEKLY`
- `MONTHLY`
- `QUARTERLY`

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "loanNumber": "LN-ABCD-000001",
    "borrowerId": "uuid",
    "borrowerName": "John Doe",
    "principalAmount": 100000.00,
    "interestRate": 12.00,
    "interestType": "REDUCING_BALANCE",
    "tenureMonths": 12,
    "emiAmount": 8884.88,
    "totalInterest": 6618.56,
    "totalPayable": 106618.56,
    "status": "DRAFT"
  }
}
```

### GET /v1/loans

List loans with pagination.

### GET /v1/loans/{id}

Get loan by ID with repayment schedule.

### GET /v1/loans/number/{loanNumber}

Get loan by loan number.

### GET /v1/loans/borrower/{borrowerId}

Get all loans for a borrower.

### GET /v1/loans/status/{status}

Get loans by status.

**Loan Statuses:**
- `DRAFT` - Initial state
- `PENDING_APPROVAL` - Awaiting approval
- `APPROVED` - Approved, pending disbursement
- `REJECTED` - Loan rejected
- `ACTIVE` - Disbursed and active
- `CLOSED` - Fully paid
- `WRITTEN_OFF` - Bad debt written off
- `CANCELLED` - Cancelled before disbursement

### POST /v1/loans/{id}/approve

Approve a loan.

**Query Parameters:**
- `notes` - Approval notes

### POST /v1/loans/{id}/disburse

Disburse an approved loan.

**Request:**
```json
{
  "disbursementDate": "2024-01-15",
  "firstPaymentDate": "2024-02-15"
}
```

**Response includes generated schedule:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "ACTIVE",
    "disbursementDate": "2024-01-15",
    "schedules": [
      {
        "installmentNumber": 1,
        "dueDate": "2024-02-15",
        "principalComponent": 7884.88,
        "interestComponent": 1000.00,
        "installmentAmount": 8884.88,
        "outstandingAfter": 92115.12,
        "status": "PENDING"
      }
    ]
  }
}
```

### GET /v1/loans/{id}/schedule

Get repayment schedule for a loan.

---

## Payment Endpoints

### POST /v1/payments

Record a payment.

**Request:**
```json
{
  "loanId": "uuid",
  "amount": 10000.00,
  "paymentDate": "2024-02-15",
  "paymentMethod": "BANK_TRANSFER",
  "referenceNumber": "TXN123456",
  "transactionId": "BANK-REF-001",
  "idempotencyKey": "unique-request-id",
  "notes": "February payment"
}
```

**Payment Methods:**
- `CASH`
- `BANK_TRANSFER`
- `MOBILE_MONEY`
- `CHEQUE`
- `CARD`
- `WALLET`
- `OTHER`

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "paymentNumber": "PAY-ABCD-000001",
    "loanId": "uuid",
    "amountPaid": 10000.00,
    "principalPaid": 7884.88,
    "interestPaid": 1000.00,
    "penaltyPaid": 0,
    "excessAmount": 1115.12,
    "status": "COMPLETED",
    "receiptNumber": "RCP-ABCD-000001",
    "allocations": [
      {
        "installmentNumber": 1,
        "dueDate": "2024-02-15",
        "principalAllocated": 7884.88,
        "interestAllocated": 1000.00,
        "totalAllocated": 8884.88
      }
    ]
  }
}
```

### GET /v1/payments

List all payments with pagination.

### GET /v1/payments/{id}

Get payment by ID with allocation details.

### GET /v1/payments/loan/{loanId}

Get payments for a specific loan.

### POST /v1/payments/{id}/reverse

Reverse a payment (LENDER_ADMIN only).

**Query Parameters:**
- `reason` - Reason for reversal

---

## Error Codes

| Code | Description |
|------|-------------|
| `INVALID_CREDENTIALS` | Invalid email or password |
| `ACCOUNT_LOCKED` | Account is locked due to failed attempts |
| `ACCOUNT_INACTIVE` | Account is not active |
| `TOKEN_EXPIRED` | JWT token has expired |
| `INVALID_TOKEN` | Invalid JWT token |
| `ACCESS_DENIED` | Insufficient permissions |
| `RESOURCE_NOT_FOUND` | Requested resource not found |
| `BUSINESS_CODE_EXISTS` | Business code already exists |
| `EMAIL_EXISTS` | Email already exists |
| `PHONE_EXISTS` | Phone number already exists |
| `BORROWER_INACTIVE` | Cannot create loan for inactive borrower |
| `INVALID_LOAN_STATUS` | Operation not allowed in current loan status |
| `LOAN_NOT_ACTIVE` | Cannot record payment for inactive loan |
| `OUTSTANDING_BALANCE` | Cannot close loan with outstanding balance |
| `ALREADY_REVERSED` | Payment is already reversed |
| `LIMIT_EXCEEDED` | Tenant limit exceeded |
| `VALIDATION_ERROR` | Request validation failed |
| `INTERNAL_ERROR` | Unexpected server error |

---

## Rate Limiting

API rate limiting is applied per tenant:

- **Standard Plan**: 100 requests/minute
- **Premium Plan**: 500 requests/minute
- **Enterprise Plan**: Unlimited

Rate limit headers:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705312800
```

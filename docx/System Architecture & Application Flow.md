# System Architecture & Application Flow

## Multi-Tenant Loan Management Platform (Spring Boot)

---

## 1. Architecture Goals

The system architecture is designed to:

* Support **multi-tenant SaaS** with strict data isolation
* Handle **financial-grade consistency and auditability**
* Scale from **small lenders to enterprise usage**
* Support **manual + automated lending workflows**
* Remain **simple to operate initially** and extensible later

Key principles:

* Modular monolith (Spring Boot) first
* Clear domain boundaries
* Strong transactional guarantees
* Asynchronous processing where needed

---

## 2. High-Level System Architecture

```
                ┌───────────────────────┐
                │   Web / Mobile UI     │
                │  (Admin, Lender)      │
                └───────────┬───────────┘
                            │ HTTPS
                ┌───────────▼───────────┐
                │      API Gateway       │
                │ Auth, Rate Limit, CORS │
                └───────────┬───────────┘
                            │
        ┌───────────────────▼───────────────────┐
        │         Spring Boot Application        │
        │ (Modular Monolith, Multi-Tenant)      │
        │                                       │
        │ Auth | Tenant | Loan | Payment |      │
        │ Ledger | Collection | Reporting       │
        └───────┬───────────────┬───────────────┘
                │               │
     ┌──────────▼─────────┐   ┌─▼──────────────┐
     │ PostgreSQL (RLS)   │   │ Redis / Queue  │
     │ Tenant-aware DB    │   │ Cache & Jobs   │
     └──────────┬─────────┘   └─┬──────────────┘
                │               │
     ┌──────────▼─────────┐   ┌─▼──────────────┐
     │ Object Storage     │   │ External APIs  │
     │ Docs / Agreements  │   │ SMS / Email    │
     └────────────────────┘   └────────────────┘
```

---

## 3. Backend Logical Architecture (Spring Boot)

### 3.1 Modular Monolith Structure

Each module is **logically isolated** but deployed together:

```
loan-platform
 ├── auth-service
 ├── tenant-service
 ├── borrower-service
 ├── loan-service
 ├── payment-service
 ├── ledger-service
 ├── collection-service
 ├── reporting-service
 ├── notification-service
 ├── scheduler-service
 └── audit-service
```

This avoids distributed complexity while allowing future service extraction.

---

## 4. Multi-Tenancy Architecture

### 4.1 Tenant Resolution Flow

1. User logs in
2. JWT token issued containing:

   * user_id
   * tenant_id
   * role
3. Each request passes through **Tenant Filter**
4. Tenant ID injected into:

   * ThreadLocal context
   * Hibernate session filter
5. All DB queries automatically scoped to tenant

### 4.2 Data Isolation Levels

| Layer       | Isolation                |
| ----------- | ------------------------ |
| Application | TenantContext validation |
| ORM         | Hibernate tenant filter  |
| Database    | PostgreSQL RLS           |
| Storage     | Tenant-prefixed buckets  |

---

## 5. Application Flow – End to End

---

## 5.1 Super Admin → Lender Onboarding Flow

```
Super Admin
   │
   ▼
Create Lender Account
   │
   ▼
Generate Tenant ID
   │
   ▼
Create Admin User for Tenant
   │
   ▼
Send Invite / Credentials
   │
   ▼
Lender Activated
```

System Actions:

* Tenant record created
* Default configurations seeded
* Usage limits assigned
* Audit log recorded

---

## 5.2 Lender → Borrower Onboarding Flow

```
Lender
  │
  ▼
Add Borrower Details
  │
  ▼
Upload Documents
  │
  ▼
Assign Risk Rating
  │
  ▼
Borrower Active
```

System Actions:

* Borrower profile stored
* Documents saved to tenant storage
* Risk score initialized

---

## 5.3 Loan Creation & Disbursement Flow

```
Lender
  │
  ▼
Create Loan Draft
  │
  ▼
System Calculates EMI & Schedule
  │
  ▼
Review & Confirm
  │
  ▼
(Optional) Approval Required?
  │          │
  │          ▼
  │      Approver Reviews
  │          │
  └──────────┘
  ▼
Loan Disbursed
```

System Actions:

* Loan persisted
* Repayment schedule generated (immutable)
* Ledger entries created
* Notifications sent

---

## 5.4 Repayment & Payment Allocation Flow

```
Borrower Pays
  │
  ▼
Payment Recorded
  │
  ▼
Allocation Engine
  │  (Penalty → Interest → Principal)
  ▼
Schedule Updated
  │
  ▼
Ledger Entries Created
  │
  ▼
Receipt Generated
```

System Actions:

* Partial/advance handling
* Outstanding recalculated
* Payment confirmation sent

---

## 5.5 Daily Scheduler & Automation Flow

```
Scheduler Trigger
  │
  ├─► Interest Accrual
  ├─► Due Date Checks
  ├─► Overdue Detection
  ├─► Penalty Calculation
  ├─► Notifications
  └─► Dashboard Metrics Refresh
```

Runs tenant-aware jobs safely in batches.

---

## 5.6 Overdue & Collections Flow

```
Installment Missed
  │
  ▼
Mark Overdue
  │
  ▼
Assign DPD Bucket
  │
  ▼
Send Alerts
  │
  ▼
Collector Follow-up
  │
  ▼
Resolution / Write-off
```

---

## 5.7 Loan Closure Flow

```
Outstanding = 0
  │
  ▼
Close Loan
  │
  ▼
Final Ledger Posting
  │
  ▼
Generate NOC / Statement
  │
  ▼
Archive Loan
```

---

## 6. Error Handling & Idempotency

* Idempotency keys for payments
* Retry-safe scheduler jobs
* Compensating transactions for failures
* Manual overrides with audit trail

---

## 7. Scalability Considerations

* Read replicas for reports
* Async notification processing
* Horizontal scaling via containers
* Future service extraction ready

---

## 8. Observability & Monitoring

* Centralized logging (ELK)
* Metrics (Prometheus + Grafana)
* Distributed tracing
* Business KPIs (PAR, DPD)

---

## 9. Security Flow Summary

```
Request
  │
  ▼
JWT Validation
  │
  ▼
Tenant Context Injection
  │
  ▼
Role Authorization
  │
  ▼
Service Execution
```

---

## 10. Final Notes

This architecture:

* Matches real-world lending operations
* Avoids premature microservices complexity
* Supports compliance, audits, and growth
* Aligns perfectly with Spring Boot strengths

---

**Document Type:** System Architecture & Application Flow
**Backend:** Java Spring Boot 3.x
**Status:** Implementation-Ready

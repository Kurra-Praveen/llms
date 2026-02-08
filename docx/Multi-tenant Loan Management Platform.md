# Multi-Tenant Loan Management Platform

## SaaS Platform for Small-Scale Lenders (Updated – Spring Boot Architecture)

---

## 1. Executive Summary

This document defines the **updated and enhanced functional, business, and technical architecture** for a **multi-tenant Loan Management SaaS platform** targeted at small-scale lenders (micro-lenders, informal lenders, MSMEs, and peer-to-peer lending operators).

The platform enables a **Super Admin (platform owner)** to onboard and manage multiple independent lenders (tenants), while each lender can independently manage borrowers, loans, payments, collections, and reports — all with **strict tenant-level data isolation**.

This version **extends the original design** by:

* Adding accounting-grade **ledger & audit systems**
* Introducing **collections, approvals, restructuring, and risk scoring**
* Replacing the backend with **Java 17 + Spring Boot 3.x**
* Refining scalability, security, and SaaS monetization readiness

---

## 2. Platform Vision

Build a **white-label, enterprise-grade lending management system** that:

* Requires **no technical knowledge** from lenders
* Supports **manual + semi-digital lending workflows**
* Can scale from **10 loans to millions of records**
* Acts as the **system of record** for lending, accounting, and collections

---

## 3. User Roles & Hierarchy

### 3.1 Roles

| Role                  | Scope     | Responsibilities                                      |
| --------------------- | --------- | ----------------------------------------------------- |
| Super Admin           | Platform  | Manage tenants, subscriptions, system configs, audits |
| Lender                | Tenant    | Manage borrowers, loans, payments, reports            |
| Lender Staff (Future) | Tenant    | Restricted operational access                         |
| Borrower              | Read-only | View loans, schedules, payments (optional portal)     |

---

## 4. Core Business Modules (Enhanced)

### 4.1 Borrower Management

* Borrower onboarding & profiling
* Document uploads (ID, income, address proof)
* Internal credit rating & risk banding
* Borrower history & communication logs
* Block / deactivate borrower

### 4.2 Loan Management

#### Loan Types Supported

* Flat interest loans
* Reducing balance loans (EMI)
* Simple interest loans
* Interest-only loans
* Bullet / balloon loans

#### Advanced Loan Features

* Grace period & moratorium
* Processing fees & charges
* Collateral tracking (optional)
* Early closure & prepayment
* Loan restructuring (tenure/EMI changes)
* Write-off handling

---

## 5. Repayment Schedule & Interest Engine

* Automated amortization schedule generation
* Supports daily / weekly / monthly frequencies
* Configurable interest accrual (daily/monthly)
* Penalty engine:

  * Fixed penalty
  * Percentage-based
  * Tiered escalation

Schedules are **immutable once disbursed** unless restructured.

---

## 6. Payment Processing & Allocation

### Payment Waterfall (System Enforced)

1. Penalties
2. Overdue Interest
3. Current Interest
4. Principal

### Supported Scenarios

* Partial payments
* Advance payments
* Bulk uploads (Excel)
* Payment reversals (approval required)
* Manual & digital payment entries

---

## 7. Accounting & Ledger System (New)

### Purpose

Provide **audit-ready, double-entry accounting** for all financial transactions.

### Ledger Design

**Ledger Accounts**

* Cash / Bank (Asset)
* Loan Principal Outstanding (Asset)
* Interest Receivable (Asset)
* Interest Income (Income)
* Penalty Income (Income)
* Processing Fee Income (Income)

**Every financial event generates immutable ledger entries.**

Example – Payment Received:

```
Dr Cash / Bank
    Cr Interest Receivable
    Cr Loan Principal Outstanding
    Cr Penalty Income
```

---

## 8. Collections & Dunning (New)

### Overdue Buckets

| Stage             | Days Past Due |
| ----------------- | ------------- |
| Soft Reminder     | 1–7           |
| Follow-up         | 8–30          |
| Hard Reminder     | 31–60         |
| Legal / Write-off | 60+           |

### Features

* Automated bucket assignment
* Promise-to-pay tracking
* Collector notes
* Escalation history

---

## 9. Risk & Credit Scoring (Internal)

* Rule-based internal risk scoring
* Inputs: income, repayment history, overdue frequency
* Risk bands: Low / Medium / High
* Used for loan eligibility & approval limits

---

## 10. Maker–Checker & Approval Workflow

### Approval Required For:

* Loan disbursement (optional)
* Payment reversals
* Penalty waivers
* Loan write-offs

Supports:

* Approval queue
* Audit trail
* Role-based authorization

---

## 11. Reporting & Analytics

### Standard Reports

* Portfolio summary
* Aging (PAR buckets)
* Collections efficiency
* Interest income
* Borrower performance
* Cash flow forecast

### Export Formats

* PDF
* Excel

---

## 12. Notifications & Communication

* SMS, Email, In-app notifications
* Configurable reminder rules
* Template customization per tenant
* Notification delivery logs

---

## 13. Multi-Tenancy & Data Isolation

### Model

* Shared PostgreSQL database
* Mandatory `tenant_id` column
* Tenant context injected per request
* Hibernate-level filtering

Ensures **zero cross-tenant data leakage**.

---

## 14. Security & Compliance

* TLS 1.3
* JWT + Refresh tokens
* Spring Security RBAC
* AES-256 encryption at rest
* Immutable audit logs
* GDPR-ready data controls
* Daily backups + PITR

---

## 15. Backend Technology Stack (Updated)

### Core Backend

| Layer      | Technology                  |
| ---------- | --------------------------- |
| Language   | Java 21 (LTS)               |
| Framework  | Spring Boot 3.x             |
| API        | Spring Web (REST)           |
| ORM        | Spring Data JPA + Hibernate |
| Security   | Spring Security             |
| Auth       | JWT                         |
| Validation | Hibernate Validator         |
| API Docs   | Springdoc OpenAPI           |

### Infrastructure

| Component    | Technology                |
| ------------ | ------------------------- |
| Database     | PostgreSQL 15             |
| Cache        | Redis                     |
| File Storage | S3 / MinIO                |
| Scheduler    | Spring Scheduler / Quartz |
| Messaging    | Redis / Kafka (future)    |

---

## 16. Recommended Package Structure

```
com.platform.lending
 ├── auth
 ├── tenant
 ├── borrower
 ├── loan
 ├── payment
 ├── ledger
 ├── collection
 ├── reporting
 ├── notification
 ├── scheduler
 ├── audit
 └── common
```

---

## 17. Implementation Roadmap (Refined)

### Phase 1 – Core (MVP)

* Tenant onboarding
* Borrowers & loans
* Schedule engine
* Manual payments
* Notifications

### Phase 2 – Stability

* Ledger system
* Collections
* Advanced reports
* Risk scoring

### Phase 3 – Scale

* Payment gateway integration
* Borrower portal
* Maker-checker workflows
* Mobile apps

---

## 18. Final Outcome

This platform is designed to function as:

* A **SaaS business** for the platform owner
* A **core lending system** for small lenders
* A **scalable, auditable, enterprise-ready foundation**

It balances **real-world lending operations** with **modern software architecture**, making it suitable for long-term growth and regulatory readiness.

---

**Document Version:** v2.0
**Backend Stack:** Java Spring Boot 3.x
**Status:** Architecture Ready for Implementation

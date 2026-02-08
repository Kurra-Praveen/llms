# ER Diagrams & Database Flows

## Multi-Tenant Loan Management Platform (Spring Boot)

---

## Purpose

This document contains **ER diagrams, relationship descriptions, and database flow diagrams** to help design and implement the platform database and data flows.

It focuses on the **core financial domain** (tenants, borrowers, loans, schedules, payments, ledger) and includes:

* Logical ER diagram (text/ASCII)
* Entity descriptions (fields & indexes)
* Key relationships & constraints
* Database flows for major operations (loan creation, payment processing, scheduler)
* DDL snippets (PostgreSQL)
* Scaling & partitioning recommendations

---

## ER Diagram (Logical)

```
+----------------+     +----------------+      +--------------------+
|    tenants     |1---<|     users      |      |     borrowers      |
+----------------+     +----------------+      +--------------------+
| tenant_id (PK) |     | user_id (PK)   |      | borrower_id (PK)   |
| name           |     | tenant_id (FK) |      | tenant_id (FK)     |
| ...            |     | email          |      | full_name          |
+----------------+     | role           |      | phone              |
                       +----------------+      | email              |
                                              +-| id_number         |
                                              | +--------------------+
                                              |
                                              |    +----------------+
                                              |    |    loans       |
                                              |    +----------------+
                                              |    | loan_id (PK)   |
                                              +--<| tenant_id (FK) |
                                                   | borrower_id (FK)|
                                                   | principal       |
                                                   | interest_rate   |
                                                   | interest_type   |
                                                   | tenure_months   |
                                                   | status          |
                                                   +----------------+
                                                         |
                                                         |1
                                                         |   +--------------------------+
                                                         +--<| repayment_schedules      |
                                                             +--------------------------+
                                                             | schedule_id (PK)         |
                                                             | loan_id (FK)             |
                                                             | installment_number       |
                                                             | due_date                 |
                                                             | principal_component      |
                                                             | interest_component       |
                                                             | installment_amount       |
                                                             | status                  |
                                                             +--------------------------+


+----------------+     +----------------+      +--------------------+
|   payments     |     |   ledger_txns  |      | ledger_accounts    |
+----------------+     +----------------+      +--------------------+
| payment_id (PK)|     | txn_id (PK)    |      | account_id (PK)    |
| tenant_id (FK) |     | tenant_id (FK) |      | tenant_id (FK)     |
| loan_id (FK)   |     | related_payment |      | account_name       |
| amount_paid    |     | entries[]       |      | account_type       |
| payment_date   |     | created_at      |      +--------------------+
| method         |     +----------------+
+----------------+


+----------------+
| notifications  |
+----------------+
| notification_id|
| tenant_id (FK) |
| target_user_id |
| type           |
| message        |
| status         |
+----------------+
```

---

## Entities & Key Fields (short)

### tenants

* `tenant_id` UUID PK
* `business_name`, `contact_email`, `status`, `settings` (JSONB)
* Index: `tenant_id` (PK)

### users

* `user_id` UUID PK
* `tenant_id` UUID FK (nullable for SuperAdmin)
* `email`, `password_hash`, `role`, `last_login`
* Indexes: `tenant_id`, `email` (unique per tenant)

### borrowers

* `borrower_id` UUID PK
* `tenant_id` UUID FK
* `full_name`, `phone`, `email`, `id_number`, `monthly_income`, `credit_rating`
* Indexes: `tenant_id, phone`, `tenant_id, id_number`

### loans

* `loan_id` UUID PK
* `tenant_id` UUID FK
* `borrower_id` UUID FK
* `loan_number` (human readable, unique per tenant)
* `principal_amount`, `interest_rate`, `interest_type`, `tenure_months`, `repayment_frequency`
* `disbursement_date`, `first_payment_date`, `status`
* `outstanding_principal`, `outstanding_interest`, `penalty_amount`, `total_paid`
* Indexes: `tenant_id, loan_id`, `tenant_id, status`, `tenant_id, borrower_id`

### repayment_schedules

* `schedule_id` UUID PK
* `loan_id` UUID FK
* `tenant_id` UUID FK (denormalized for quick filtering)
* `installment_number`, `due_date`, `principal_component`, `interest_component`, `installment_amount`, `outstanding_after`, `status`, `paid_date`
* Indexes: `loan_id, installment_number`, `tenant_id, due_date`

### payments

* `payment_id` UUID PK
* `tenant_id` UUID FK
* `loan_id` UUID FK
* `payment_date`, `amount_paid`, `payment_method`, `reference_number`, `notes`
* `principal_paid`, `interest_paid`, `penalty_paid`
* `receipt_number`
* Indexes: `tenant_id, payment_date`, `loan_id`

### ledger_accounts

* `account_id` UUID PK
* `tenant_id` UUID FK
* `account_code`, `name`, `type` (ASSET/LIABILITY/INCOME/EXPENSE)

### ledger_txns

* `txn_id` UUID PK
* `tenant_id` UUID FK
* `txn_date`, `description`, `entries` (JSONB or normalized child table)
* Alternatively normalized `ledger_entries` table:

  * `entry_id`, `txn_id`, `account_id`, `debit`, `credit`

---

## Database Flows (Operation sequences)

### 1) Loan Creation & Disbursement Flow (DB steps)

1. Begin transaction
2. Verify `tenant_id`, `borrower_id` ownership
3. Insert `loans` record with status `disbursed_pending` or `active` if auto
4. Generate amortization schedule in memory
5. Insert `repayment_schedules` rows (batch insert)
6. Create ledger transaction (`ledger_txns` + entries) for principal disbursement

   * Debit: Loan Principal Outstanding (Asset)
   * Credit: Cash/Bank (Asset)
7. Commit transaction
8. Trigger async notification

Important:

* Use DB-level FK constraints and application-level tenant checks
* Use optimistic locking (version column) on `loans` for concurrent updates

### 2) Payment Processing Flow (DB steps)

1. Receive payment (webhook / manual entry)
2. Validate idempotency key - if already processed reject
3. Begin database transaction
4. Insert `payments` record
5. Run allocation engine (penalty → overdue interest → current interest → principal)
6. Update corresponding `repayment_schedules` rows (mark as Paid/PartiallyPaid)
7. Update `loans` outstanding fields
8. Insert `ledger_txns` (double-entry for amounts allocated)
9. Commit transaction
10. Emit events (payment_received)

Notes:

* All monetary arithmetic should use DECIMAL(18,2) or numeric to avoid float issues
* Keep money arithmetic centralized in a service

### 3) Daily Scheduler Flow (DB steps)

1. Scheduler selects active loans in batches (tenant-aware)
2. For each loan:

   * Calculate accrued interest for day
   * Update `loans.outstanding_interest`
   * If due_date reached and unpaid, mark schedule as overdue and compute penalty
   * Insert penalty ledger entries if applied
3. Generate aggregated daily metrics (for reporting)
4. Commit changes per batch

Batching & rate limits:

* Use `FOR UPDATE SKIP LOCKED` to distribute work across workers

---

## DDL Snippets (PostgreSQL)

```sql
CREATE TABLE tenants (
  tenant_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  business_name text NOT NULL,
  contact_email text,
  status text DEFAULT 'active',
  settings jsonb,
  created_at timestamptz DEFAULT now()
);

CREATE TABLE users (
  user_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenants(tenant_id),
  email text NOT NULL,
  password_hash text NOT NULL,
  role text NOT NULL,
  last_login timestamptz,
  created_at timestamptz DEFAULT now(),
  UNIQUE (tenant_id, email)
);

CREATE TABLE borrowers (
  borrower_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenants(tenant_id) NOT NULL,
  full_name text NOT NULL,
  phone text,
  email text,
  id_number text,
  monthly_income numeric(18,2),
  credit_rating text,
  created_at timestamptz DEFAULT now()
);

CREATE TABLE loans (
  loan_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenants(tenant_id) NOT NULL,
  borrower_id uuid REFERENCES borrowers(borrower_id) NOT NULL,
  loan_number text,
  principal_amount numeric(18,2) NOT NULL,
  interest_rate numeric(5,2) NOT NULL,
  interest_type text NOT NULL,
  tenure_months integer NOT NULL,
  repayment_frequency text NOT NULL,
  disbursement_date date,
  first_payment_date date,
  outstanding_principal numeric(18,2) DEFAULT 0,
  outstanding_interest numeric(18,2) DEFAULT 0,
  penalty_amount numeric(18,2) DEFAULT 0,
  total_paid numeric(18,2) DEFAULT 0,
  status text DEFAULT 'draft',
  created_at timestamptz DEFAULT now(),
  version bigint DEFAULT 1
);

CREATE TABLE repayment_schedules (
  schedule_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id uuid REFERENCES loans(loan_id) NOT NULL,
  tenant_id uuid REFERENCES tenants(tenant_id) NOT NULL,
  installment_number integer NOT NULL,
  due_date date NOT NULL,
  principal_component numeric(18,2) NOT NULL,
  interest_component numeric(18,2) NOT NULL,
  installment_amount numeric(18,2) NOT NULL,
  outstanding_after numeric(18,2) NOT NULL,
  status text DEFAULT 'pending',
  paid_date date
);

CREATE TABLE payments (
  payment_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenants(tenant_id) NOT NULL,
  loan_id uuid REFERENCES loans(loan_id) NOT NULL,
  payment_date timestamptz DEFAULT now(),
  amount_paid numeric(18,2) NOT NULL,
  payment_method text,
  reference_number text,
  principal_paid numeric(18,2) DEFAULT 0,
  interest_paid numeric(18,2) DEFAULT 0,
  penalty_paid numeric(18,2) DEFAULT 0,
  receipt_number text,
  created_at timestamptz DEFAULT now()
);

CREATE TABLE ledger_accounts (
  account_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenants(tenant_id) NOT NULL,
  account_code text NOT NULL,
  account_name text NOT NULL,
  account_type text NOT NULL
);

CREATE TABLE ledger_entries (
  entry_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  txn_id uuid NOT NULL,
  tenant_id uuid REFERENCES tenants(tenant_id) NOT NULL,
  account_id uuid REFERENCES ledger_accounts(account_id) NOT NULL,
  debit numeric(18,2) DEFAULT 0,
  credit numeric(18,2) DEFAULT 0,
  created_at timestamptz DEFAULT now()
);
```

---

## Indexing & Performance Tips

* Index on (`tenant_id`, `status`) for loans
* Index on (`tenant_id`, `due_date`) on repayment_schedules
* Partial indexes for overdue rows:

  * `CREATE INDEX idx_overdue ON repayment_schedules (loan_id) WHERE status='overdue';`
* Use `numeric(18,2)` for money
* Keep `tenant_id` denormalized in frequently queried tables for easier filtering

---

## Partitioning & Archival

* Partition `repayment_schedules` and `payments` by `tenant_id` or by date range if data grows
* Archive closed loans older than X years into cold storage table

---

## Concurrency Control

* Use `SELECT ... FOR UPDATE SKIP LOCKED` for scheduler workers
* Use optimistic locking (`version` column) for loans to avoid lost updates

---

## Sample Queries (quick)

1. Get next due installments for a tenant:

```sql
SELECT * FROM repayment_schedules
WHERE tenant_id = :tid AND status = 'pending' AND due_date <= now()::date + interval '7 days'
ORDER BY due_date;
```

2. Aggregate overdue by DPD bucket:

```sql
SELECT
 CASE
  WHEN age(now(), due_date) <= make_interval(days=>7) THEN '1-7'
  WHEN age(now(), due_date) <= make_interval(days=>30) THEN '8-30'
  ELSE '30+'
 END as dpd_bucket,
 SUM(installment_amount) as amount
FROM repayment_schedules
WHERE tenant_id = :tid AND status = 'overdue'
GROUP BY 1;
```

---

## Migration & Backups

* Use Flyway/Liquibase for schema migrations
* Use PITR backups for PostgreSQL
* Backup object storage separately

---

## Final Notes

This ER & DB flow document is designed to be **developer-friendly** and convertible into migration scripts and entity classes. If you want, I can now:

* Generate full DDL migration files (Flyway)
* Create Spring Data JPA entities for these tables
* Produce an interactive ER diagram (image) and include it in assets

Tell me which of these (or multiple) I should produce next.

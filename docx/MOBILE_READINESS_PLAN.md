# Mobile Readiness Plan — Living Document

> **Status legend:** ⬜ TODO · 🔄 IN PROGRESS · ✅ DONE
> **Last updated:** 2026-10-10
> **Scope:** Extend the Multi-Tenant Loan Platform to an Android borrower self-service app (React Native + Expo), backend-blockers-first.

---

## 1. Decisions (locked)

| Decision | Choice | Date |
|----------|--------|------|
| Mobile users | **Borrowers (self-service)** | 2026-10-10 |
| Platform | **Android only** (iOS later at no extra cost via Expo) | 2026-10-10 |
| MVP scope | Full borrower-facing feature set (loans, schedules, payments, notifications, profile) | 2026-10-10 |
| Team skills | React/TypeScript + React Native/Expo | 2026-10-10 |
| Distribution | Internal/enterprise only (EAS Build APK, no public Play Store) | 2026-10-10 |
| Mobile stack | **React Native + Expo** (Expo Router, Nativewind, TanStack Query, SecureStore) | 2026-10-10 |
| Execution order | **Backend blockers first**, then mobile app | 2026-10-10 |
| Borrower provisioning (MVP) | Staff-created credentials (OTP self-registration = fast-follow) | 2026-10-10 |
| In-app payments (MVP) | View-only + offline-payment confirmation; Razorpay/UPI behind feature flag later | 2026-10-10 |

---

## 2. Backend Work Tracker

### 🔴 P0 — Blockers (mobile cannot start without these)

| # | Item | Status | Completed | Notes |
|---|------|--------|-----------|-------|
| 1 | **Link users ↔ borrowers**: Flyway V11 `users.borrower_id` FK → `borrowers.id`; update `User` entity; add `borrowerId` JWT claim | ⬜ | — | Blocks everything |
| 2 | **Borrower-scoped authorization + portal API**: borrower context/filter (mirror `TenantAspect` pattern) + `/v1/portal/**` endpoints (my profile, my loans, my schedule, my payments) | ⬜ | — | Depends on #1 |
| 3 | **Borrower provisioning**: staff-created borrower logins (extend `POST /v1/auth/users` or auto-create on borrower creation) | ⬜ | — | Depends on #1 |
| 4 | **FCM push**: `firebase-admin` dep, real PUSH delivery in `NotificationService`, `POST/DELETE /v1/devices` token registration, wire PUSH into due-reminder/receipt/overdue notifications | ⬜ | — | Parallel with #2–3 |
| 5 | **Notification query API**: `NotificationController` (list / get / mark-read) for in-app notification center | ⬜ | — | Parallel with #4 |

### 🟠 P1 — Security hardening (before real borrowers log in)

| # | Item | Status | Completed | Notes |
|---|------|--------|-----------|-------|
| 6 | **Refresh-token rotation**: issue new refresh token on every refresh | ⬜ | — | Parallel |
| 7 | **Rate limiting** (Bucket4j) on `/v1/auth/**` + `/v1/portal/**` | ⬜ | — | Parallel |
| 8 | **Borrower-isolation integration tests**: assert borrower A cannot read borrower B's data on every portal endpoint | ⬜ | — | Depends on #2 |

### 🟡 P2 — Deferred (pending product decisions)

| # | Item | Status | Completed | Notes |
|---|------|--------|-----------|-------|
| 9 | Payment gateway (Razorpay/UPI) or record-payment-intent endpoint | ⬜ | — | Behind feature flag |
| 10 | KYC document upload (S3/MinIO + multipart endpoints) | ⬜ | — | Needs storage infra |
| 11 | HTTP caching headers, CORS externalization, Sentry, GitHub Actions CI | ⬜ | — | Anytime |

---

## 3. Key Findings (evidence-based)

### Blockers
- **B1 — No borrower-facing API.** All of `/v1/borrowers/**`, `/v1/loans/**`, `/v1/payments/**`, `/v1/reports/**` require `LENDER_ADMIN`/`LENDER_STAFF` (`SecurityConfig.java` L51–59). `BORROWER` role is an unused enum value (`Role.java` L7). No `users.borrower_id` link exists.
- **B2 — No borrower provisioning/auth flow.** Login is email/password for staff-created users only; no self-registration, OTP, or password reset.
- **B3 — Push notifications non-functional.** `PUSH` channel enum exists but `NotificationService.java` L151–157 only marks rows `SENT` with a mock ID; no FCM dependency; no device-token endpoint.

### High
- **H1** No notifications query API (no `NotificationController`).
- **H2** No payment gateway (Razorpay/PayU planned only).
- **H3** No KYC/document upload endpoints or object storage.
- **H4** No CI/CD anywhere in the workspace.

### Medium
- **M1** Refresh tokens not rotated (`AuthService.java` L138–173).
- **M2** No rate limiting/throttling anywhere.
- **M3** No borrower-scoped authorization pattern (tenant filter exists; "borrower sees ONLY own data" needs a new filter).
- **M4** No HTTP caching headers/ETag; default page 20 / max 100.
- **M5** Frontend has zero tests; backend has tests but no coverage tooling.
- **M6** API base URL hard-coded `'/api'` in `frontend/src/services/api.ts` L9.

### Low
- **L1** CORS origins hardcoded incl. test ngrok domain (`SecurityConfig.java` L92–97).
- **L2** No i18n — all strings hardcoded English.
- **L3** Observability is actuator-only; no error tracking.
- **L4** Single `/v1` API version — acceptable.

---

## 4. Architecture Snapshot

```
React web (staff)          Future Android app (borrower)
      │ axios, Bearer JWT          │ Bearer JWT — BLOCKED until P0 done
      ▼                            ▼
┌─────────────────────────────────────────────────────┐
│  Spring Boot modular monolith :4044 /api/v1          │
│  auth (JWT 1h/7d) · tenant (Hibernate @Filter)       │
│  borrower · loan (InterestCalculationEngine)         │
│  payment (PaymentAllocationEngine, idempotency)      │
│  ledger (no REST) · notification (stubbed)           │
│  scheduler (daily overdue/DPD cron) · audit (no REST)│
└─────────────────────────────────────────────────────┘
      │                              │
 PostgreSQL 15 (Flyway V1–V10)   Redis 7 (cache)
```

**Multi-tenancy:** `tenantId` JWT claim → `TenantContext` ThreadLocal → Hibernate `@Filter` on tenant-aware entities. Mobile clients inherit isolation automatically; borrower scoping adds a second filter dimension (P0 #2).

---

## 5. Roadmap (post-P0/P1)

| Phase | Scope | Milestone |
|-------|-------|-----------|
| **1 — Monorepo + shared package** (1–2 wks) | npm workspaces; `packages/shared` (types, zod, API client w/ storage adapter, EMI calc); refactor `frontend/` onto it; Expo skeleton with login | Web app runs on shared package; mobile login + silent refresh on device |
| **2 — MVP screens** (3–5 wks) | Home (active loans, next EMI), my loans, loan details + schedule, payment history, notification center, profile; i18n scaffolding | Internal dogfood APK |
| **3 — Native features & payments** (2–4 wks) | Push E2E, biometric unlock, offline cache, Razorpay/UPI (flag), optional KYC camera upload | Feature-complete internal build |
| **4 — Release & ops** (1–2 wks) | EAS Build production APK, internal distribution, GitHub Actions CI, Sentry, backend monitoring | v1.0 pilot rollout |

**Target structure:** root npm workspaces → `frontend/` (existing), `mobile/` (new Expo app), `packages/shared/` (types/validations/api/utils), `loan-platform/` (backend, unchanged), `docx/` (docs).

---

## 6. Top Risks & Mitigations

1. **Cross-borrower data leakage** → borrower-scoped filter + per-endpoint isolation tests (P1 #8) + security review before Phase 2.
2. **Scope ambiguity** → borrower feature list frozen in §1 decisions.
3. **Payment-gateway timing/compliance** → MVP view-only; gateway behind feature flag.
4. **Android OEM push unreliability** → in-app notification center as source of truth; SMS fallback (needs real SMS provider).
5. **No CI / no frontend tests** → CI in Phase 1; unit tests for shared package first.
6. **Token theft on device** → SecureStore (Keystore-backed), refresh rotation (P1 #6), biometric gate, 1h access TTL.

---

## 7. Open Questions

1. ~~Scope confirmation~~ — **Resolved:** full borrower-facing feature set.
2. ~~Borrower provisioning~~ — **Resolved (MVP):** staff-created credentials; OTP self-registration fast-follow.
3. ~~In-app payments~~ — **Resolved (MVP):** view-only + offline confirmation; gateway later.
4. **KYC upload in MVP?** — Deferred (P2 #10). Needs S3/MinIO.
5. **Languages at launch?** — English first; i18n scaffolding built in Phase 2.
6. **Production API hosting?** — Only localhost/Docker configs exist; prod deployment config TBD.

---

## 8. Change Log

| Date | Change |
|------|--------|
| 2026-10-10 | Document created. Backend-first execution approved. P0 #1 started. |

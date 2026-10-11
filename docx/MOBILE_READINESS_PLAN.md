# Mobile Readiness Plan — Living Document

> **Status legend:** ⬜ TODO · 🔄 IN PROGRESS · ✅ DONE
> **Last updated:** 2026-10-10 (evening — P0 + P1 complete)
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
| 1 | **Link users ↔ borrowers**: Flyway V11 `users.borrower_id` FK → `borrowers.id`; update `User` entity; add `borrowerId` JWT claim | ✅ | 2026-10-10 | `V11__link_users_to_borrowers.sql`, `User.java`, `JwtService.java`, `UserPrincipal.java`, `AuthResponse.java` |
| 2 | **Borrower-scoped authorization + portal API**: borrower context/filter (mirror `TenantAspect` pattern) + `/v1/portal/**` endpoints (my profile, my loans, my schedule, my payments) | ✅ | 2026-10-10 | New `portal/` module: `PortalController` + `PortalService` + `SecurityUtils.requireBorrowerId()`; ownership checks return 404 (not 403) to avoid leaking existence; `/v1/portal/**` gated to `BORROWER` role in `SecurityConfig.java` |
| 3 | **Borrower provisioning**: staff-created borrower logins (extend `POST /v1/auth/users` or auto-create on borrower creation) | ✅ | 2026-10-10 | `CreateUserRequest.borrowerId` + validation in `AuthService.createUser()` (borrower must exist in tenant, one login per borrower) |
| 4 | **FCM push**: `firebase-admin` dep, real PUSH delivery in `NotificationService`, `POST/DELETE /v1/devices` token registration, wire PUSH into due-reminder/receipt/overdue notifications | ✅ | 2026-10-10 | `PushNotificationService` (disabled by default; enable via `FIREBASE_ENABLED=true` + credentials), `DeviceToken` entity + V12 migration, `DeviceController`, PUSH added to all 4 notification types |
| 5 | **Notification query API**: `NotificationController` (list / get / mark-read) for in-app notification center | ✅ | 2026-10-10 | `GET /v1/notifications`, `GET /v1/notifications/unread-count`, `POST /v1/notifications/{id}/read`; `read_at` column in V12 |

### 🟠 P1 — Security hardening (before real borrowers log in)

| # | Item | Status | Completed | Notes |
|---|------|--------|-----------|-------|
| 6 | **Refresh-token rotation**: issue new refresh token on every refresh | ✅ | 2026-10-10 | Rotation was already implemented; **added reuse detection** — replaying a revoked token revokes the user's whole token family (`AuthService.refreshToken()`) |
| 7 | **Rate limiting** (Bucket4j) on `/v1/auth/**` + `/v1/portal/**` | ✅ | 2026-10-10 | `RateLimitingFilter` — per-IP in-memory buckets: 10 req/min auth, 60 req/min portal (env-configurable). Note: single-instance; use Redis-backed buckets if scaling out |
| 8 | **Borrower-isolation integration tests**: assert borrower A cannot read borrower B's data on every portal endpoint | ✅ | 2026-10-10 | `PortalControllerIntegrationTest` — 12 tests: own-data access (5), cross-borrower isolation (4), role enforcement (3). All passing |

### 🟡 P2 — Deferred (pending product decisions)

| # | Item | Status | Completed | Notes |
|---|------|--------|-----------|-------|
| 9 | Payment gateway (Razorpay/UPI) or record-payment-intent endpoint | ⬜ | — | Behind feature flag |
| 10 | KYC document upload (S3/MinIO + multipart endpoints) | ⬜ | — | Needs storage infra |
| 11 | HTTP caching headers, CORS externalization, Sentry, GitHub Actions CI | ⬜ | — | Anytime |

**Test status:** `mvn test` → **127 tests, 0 failures, 0 errors** (115 pre-existing + 12 new portal isolation tests). Also fixed a pre-existing broken stub in `LoanServiceTest` (4-arg → 5-arg `calculateEmi` after V10).

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

## 5. Roadmap — RESTRUCTURED 2026-10-10 (functional parity first, push last)

> **Guiding principle (user decision):** The mobile app must function like the working web frontend — a functional prototype borrowers can actually use. Push notifications (Firebase project + extra setup) are deferred to the end. Core functionality > native bells & whistles.

### ✅ Completed
- **Backend P0+P1** — portal API, provisioning, FCM service (dormant), rate limiting, token rotation, isolation tests. 127/127 green.
- **Web frontend** — borrower portal, role routing, notification center, staff "Create Login".
- **Mobile MVP** — Expo app live on device: login → Home → Loans → Loan Details → Payments → Profile. Auth + auto-refresh + SecureStore working.

### 🔄 Remaining — reordered by "make it function like the frontend"

| # | Feature | Why (parity gap) | Effort | Status |
|---|---------|------------------|--------|--------|
| 1 | **Notifications screen** | Web has a notification center + unread badge; mobile has none. Backend API + service already exist — just needs a mobile screen (bell icon + list + mark-read). | S | ⬜ |
| 2 | **Pull-to-refresh + loading/empty/error states everywhere** | Web handles these; mobile needs consistent UX on all screens. | S | ⬜ |
| 3 | **Offline tolerance** (TanStack Query persistence) | App should show cached data on flaky networks instead of blank screens. | S–M | ⬜ |
| 4 | **Biometric unlock** (`expo-local-authentication`) | Fingerprint/face unlock instead of re-login — expected on mobile. | S | ⬜ |
| 5 | **Form validation polish** (zod schemas shared with web) | Reuse the web's validation rules for any future input screens. | S | ⬜ |
| 6 | **EAS Build APK** (internal distribution) | Installable APK — no Expo Go needed for testers/pilot borrowers. | S | ⬜ |
| 7 | **Monorepo shared package** (`packages/shared`) | Eliminate type/API duplication between `frontend/` and `mobile/`. (Deferred — types currently duplicated in `mobile/lib/types.ts`.) | M | ⬜ |
| 8 | **i18n scaffolding** | Regional languages for borrowers (English first). | M | ⬜ |
| 9 | **Push notifications (FCM E2E)** | Firebase project + `google-services.json` + device registration + real delivery. Backend service is built but dormant (`FIREBASE_ENABLED=false`). **Intentionally last** — needs external Firebase setup. | M | ⬜ |
| 10 | **Payment gateway (Razorpay/UPI)** | In-app self-payment. Behind feature flag. (P2 — product decision.) | M–L | ⬜ |
| 11 | **KYC document upload** | Camera + S3/MinIO. (P2 — product decision.) | M | ⬜ |

### Milestones
- **M1 — Functional parity** (items 1–4): mobile app matches the web portal's core functionality. *Target: next session.*
- **M2 — Distributable prototype** (items 5–6): installable APK for internal testing.
- **M3 — Production hardening** (items 7–8): shared package, i18n.
- **M4 — Native features** (items 9–11): push, payments, KYC.

**Target structure (deferred to M3):** root npm workspaces → `frontend/`, `mobile/`, `packages/shared/`, `loan-platform/`, `docx/`.

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
| 2026-10-10 | **P0 (all 5) + P1 (all 3) completed and test-verified.** New: `portal/` module (7 endpoints), notification center API, device registration, FCM push service, V11+V12 migrations, rate limiting, token reuse detection, 12 isolation tests. `mvn test`: 127/127 green. **Backend is mobile-ready.** |
| 2026-10-10 | **Frontend updated for borrower portal.** New: `portalService`/`notificationService`/`deviceService`, 5 portal pages (`/portal`, `/portal/loans`, `/portal/loans/:id`, `/portal/payments`, `/notifications`), role-based routing (BORROWER → portal, staff → dashboard), live unread-count badge in Header, staff "Create Login" modal on borrower details. `borrowerId` added to `User` type. New files are TS-clean and ESLint-clean (33 pre-existing form errors unchanged). **Web app now supports borrower self-service.** |
| 2026-10-10 | **🎉 MOBILE MVP LIVE ON DEVICE.** Expo SDK 57 app in `mobile/` — Expo Router (file-based), TanStack Query, Axios with auto-refresh on 401, SecureStore (Keystore-backed) token storage, auth gate. Screens: login, Home (summary + next EMI), My Loans, Loan Details + schedule, Payments, Profile (logout). API client points to LAN IP `192.168.31.210:4044`. **Verified: borrower login on physical Android 16 device via Expo Go → navigates to Home with live data.** TypeScript compiles clean. |

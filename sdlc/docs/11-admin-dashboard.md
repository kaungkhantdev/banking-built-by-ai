# Admin Dashboard — Features & Implementation

> Digital Banking Platform · Operator Web Console
> Status: Baseline · Owner: Frontend Lead
> Runtime: Angular 22 · TypeScript · Tailwind CSS 4 · Static build (S3 + CloudFront)

---

## Overview

The admin dashboard is the **operator console** — the internal web app that
support, operations, and compliance staff use to search customers, inspect
entities, and take **audited, RBAC-gated actions** (suspend an account, freeze a
wallet, reverse a transaction, override a KYC decision). It is built with
**Angular 22 + TypeScript**, styled with **Tailwind CSS 4**, and shipped as a
**static build** to S3 + CloudFront (see `08-deployment-infra.md`) — it runs no
server of its own.

Like the mobile app, it is a **thin, typed client over the `/v1` REST API** and
holds **no business logic**. Three rules govern it:

1. **RBAC is a server guarantee; the UI only mirrors it.** The console hides or
   disables actions the operator's role lacks — purely for usability — but the
   backend independently enforces every permission via `@PreAuthorize`. UI hiding
   is **convenience, not security**.
2. **Every operator action is audited.** Sensitive actions require a typed
   **reason** and produce an `AuditRecord` (actor + reason + before/after +
   `trace_id`) on the backend. The UI is built around capturing that reason.
3. **The API is the source of truth.** All state is server state; the console
   caches and renders it, and surfaces the standard error envelope
   `{code, message, trace_id}` for support correlation.

Each feature is documented **(a) what**, **(b) why**, **(c) how**, **(d) code**.

---

## 1. Project Structure & Architecture

**What.** A **feature-first** Angular app: each capability (auth, search,
customers, wallets, transactions, kyc, audit) owns its routes, components,
services, and types. Shared plumbing lives in `core/`; the design system in
`shared/`.

**Why.** It mirrors the backend's package-by-feature layout. A change to
"reverse a transaction" lives entirely in `features/transactions/`.

**How — the tree.**

```text
src/app/
├── core/
│   ├── models/
│   │   └── api.models.ts            # all TypeScript interfaces (DTOs)
│   ├── services/
│   │   └── auth.service.ts          # JWT tokens in memory + signals for auth state
│   ├── interceptors/
│   │   ├── auth.interceptor.ts      # HttpInterceptorFn: attaches Bearer token
│   │   └── error.interceptor.ts     # HttpInterceptorFn: 401/403/500 handling + toast
│   └── guards/
│       └── auth.guard.ts            # CanActivateFn: redirects to /login if unauthenticated
│
├── layout/
│   ├── shell.component.ts           # sidebar (slate-900) + topbar layout
│   └── auth-layout.component.ts     # centered auth form layout
│
├── shared/                          # BadgeComponent, SpinnerComponent, ToastComponent, PaginationComponent
│
└── features/
    ├── auth/                        # login, silent refresh
    ├── dashboard/                   # home: health panel + summary cards
    ├── accounts/                    # customer detail, suspend/close
    ├── wallets/                     # wallet detail, freeze/unfreeze
    ├── transfers/                   # transaction detail, reversal
    ├── kyc/                         # case review, compliance override
    └── audit/                       # per-entity audit trail viewer
```

**Angular 22 conventions used throughout.**

| Concern         | Choice                              | Why                                                          |
|-----------------|-------------------------------------|--------------------------------------------------------------|
| Components      | Standalone (implied by `imports:`)  | No `NgModule` boilerplate; treeshakeable.                    |
| State           | `signal()`, `computed()`, `toSignal()` | Fine-grained reactivity without RxJS in templates.        |
| DI              | `inject()` (not constructor)        | Works in any context; cleaner with signals.                  |
| Control flow    | `@if`, `@for` (not `*ngIf`/`*ngFor`) | Built-in, faster, no import needed.                        |
| HTTP            | `HttpClient` + `RxJS`               | Angular's native HTTP client with interceptor support.       |
| Routing         | `loadComponent` lazy routes         | Route-level code splitting; guards as `CanActivateFn`.       |
| Forms           | Reactive Forms + validators         | Reason capture + client validation (server re-validates).    |
| Styling         | Tailwind CSS 4                      | Utility-first; sidebar slate-900, primary indigo-600.        |
| Proxy           | `proxy.conf.json` → `:8080`         | Dev proxies `/v1/*` to the Spring Boot API.                  |

No NgRx, no SSR — this is an internal SPA over a small REST surface (YAGNI).

---

## 2. Authentication & In-Memory Session

**What.** Operators log in via `/v1/auth/login`; the access JWT is held **in a
signal** inside `AuthService` and the refresh token in a **`Secure; HttpOnly;
SameSite` cookie** set by the backend. Silent refresh on `401` is handled by the
`auth.interceptor.ts`.

**Why — why a cookie here.** A browser has no secure enclave. The safest place
for a long-lived refresh token in a web app is an **HttpOnly cookie** the
JavaScript cannot read (mitigating XSS token theft), scoped and `SameSite` to
blunt CSRF. The short-lived access token stays in a signal and is cleared on tab
close / logout.

**Code — AuthService with signals.**

```typescript
// core/services/auth.service.ts
import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';

interface TokenPair { accessToken: string; perms: string[]; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly _accessToken = signal<string | null>(null);
  private readonly _perms = signal<Set<string>>(new Set());

  readonly isAuthenticated = computed(() => this._accessToken() !== null);
  readonly perms = this._perms.asReadonly();

  login(email: string, password: string) {
    return this.http.post<TokenPair>('/v1/auth/login', { email, password }).pipe(
      tap(pair => this.setSession(pair)),
    );
  }

  refresh() {
    // Backend reads the HttpOnly refresh cookie automatically
    return this.http.post<TokenPair>('/v1/auth/refresh', {}).pipe(
      tap(pair => this.setSession(pair)),
    );
  }

  logout() {
    this._accessToken.set(null);
    this._perms.set(new Set());
    this.http.post('/v1/auth/logout', {}).subscribe();
  }

  getAccessToken() { return this._accessToken(); }

  private setSession(pair: TokenPair) {
    this._accessToken.set(pair.accessToken);
    this._perms.set(new Set(pair.perms));
  }
}
```

---

## 3. Interceptors

**Code — auth interceptor (attaches Bearer token, handles 401 with single-flight refresh).**

```typescript
// core/interceptors/auth.interceptor.ts
import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

let refreshInFlight$: ReturnType<AuthService['refresh']> | null = null;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getAccessToken();
  const authed = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authed).pipe(
    catchError(err => {
      if (err.status !== 401 || req.url.includes('/auth/')) {
        return throwError(() => err);
      }
      refreshInFlight$ ??= auth.refresh();
      return refreshInFlight$.pipe(
        switchMap(() => {
          refreshInFlight$ = null;
          const retryToken = auth.getAccessToken();
          return next(req.clone({ setHeaders: { Authorization: `Bearer ${retryToken}` } }));
        }),
      );
    }),
  );
};
```

**Code — error interceptor (toast on 4xx/5xx).**

```typescript
// core/interceptors/error.interceptor.ts
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../../shared/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);
  return next(req).pipe(
    catchError(err => {
      const msg = err.error?.message ?? 'Something went wrong';
      toast.show({ type: 'error', message: msg });
      return throwError(() => err);
    }),
  );
};
```

---

## 4. Auth Guard

```typescript
// core/guards/auth.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? true : router.createUrlTree(['/login']);
};
```

---

## 5. RBAC in the UI (Mirror, Not Gate)

**What.** A `hasPermission` computed signal and a `*ngIf`-equivalent `@if` block
drive what the operator can **see**. Both route-level guards and inline action
buttons consult the same permission set decoded from the JWT.

**Why.** Showing a "Reverse transaction" button to someone who will get a `403`
is bad UX; hiding it is good UX. But the button being hidden is **not** what
keeps money safe — the backend's `@PreAuthorize("hasAuthority('transaction:reverse')")` is.

**Code — permission helper + usage.**

```typescript
// shared/permission.ts
import { inject } from '@angular/core';
import { computed } from '@angular/core';
import { AuthService } from '../core/services/auth.service';

export function hasPermission(perm: string) {
  const auth = inject(AuthService);
  return computed(() => auth.perms().has(perm));
}
```

```typescript
// features/wallets/wallet-detail.component.ts
import { Component, inject, computed } from '@angular/core';
import { hasPermission } from '../../shared/permission';

@Component({
  selector: 'app-wallet-detail',
  template: `
    @if (canFreeze()) {
      <button (click)="openFreezeDialog()">Freeze wallet</button>
    }
  `,
})
export class WalletDetailComponent {
  protected readonly canFreeze = hasPermission('wallet:manage');

  openFreezeDialog() { /* ... */ }
}
```

---

## 6. Global Search

**What.** A single search surface over customers, wallets, transactions, and KYC
cases, routing to typed detail screens.

**Code — search service.**

```typescript
// features/search/search.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { Subject } from 'rxjs';

type Hit =
  | { type: 'customer'; id: string; email: string; status: string }
  | { type: 'wallet'; id: string; currency: string; status: string }
  | { type: 'transaction'; id: string; amount: string; status: string }
  | { type: 'kyc'; id: string; customerId: string; status: string };

@Injectable({ providedIn: 'root' })
export class SearchService {
  private readonly http = inject(HttpClient);
  private readonly term$ = new Subject<string>();

  readonly results$ = this.term$.pipe(
    debounceTime(250),
    distinctUntilChanged(),
    switchMap(q => q.length >= 2
      ? this.http.get<Hit[]>(`/v1/search?q=${encodeURIComponent(q)}`)
      : []
    ),
  );

  search(term: string) { this.term$.next(term); }
}
```

---

## 7. Entity Detail & Audit Trail

**What.** Each entity has a detail screen showing current state **plus its
append-only audit trail** from `/v1/audit`.

**Code — audit trail component.**

```typescript
// features/audit/audit-trail.component.ts
import { Component, input, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';

interface AuditRecord {
  id: string; actorId: string; action: string;
  entityType: string; entityId: string;
  before: unknown; after: unknown; traceId: string; createdAt: string;
}

@Component({
  selector: 'app-audit-trail',
  template: `
    @for (record of records(); track record.id) {
      <div class="border-b py-2">
        <span class="font-medium">{{ record.action }}</span>
        <span class="text-slate-500 text-sm ml-2">{{ record.createdAt }}</span>
      </div>
    }
  `,
})
export class AuditTrailComponent {
  readonly entityType = input.required<string>();
  readonly entityId = input.required<string>();

  private readonly http = inject(HttpClient);

  protected readonly records = toSignal(
    this.http.get<AuditRecord[]>(
      `/v1/audit?entityType=${this.entityType()}&entityId=${this.entityId()}`
    ),
    { initialValue: [] },
  );
}
```

---

## 8. Operator Actions (Audited Mutations)

**What.** State-changing operations, each of which maps to a guarded backend
endpoint and **requires a typed reason**:

| Action                    | Endpoint                              | Permission             |
|---------------------------|---------------------------------------|------------------------|
| Suspend / close account   | `POST /v1/accounts/{id}/...`          | `account:manage`       |
| Freeze / unfreeze wallet  | `POST /v1/wallets/{id}/freeze`        | `wallet:manage`        |
| Reverse transaction       | `POST /v1/transactions/{id}/reverse`  | `transaction:reverse`  |
| KYC compliance override   | `POST /v1/kyc/{id}/decision`          | `kyc:override`         |

**Code — freeze wallet with mandatory reason.**

```typescript
// features/wallets/freeze-wallet.component.ts
import { Component, input, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { hasPermission } from '../../shared/permission';

@Component({
  selector: 'app-freeze-wallet',
  imports: [ReactiveFormsModule],
  template: `
    @if (canFreeze()) {
      <button (click)="dialogOpen.set(true)">Freeze wallet</button>
    }

    @if (dialogOpen()) {
      <div class="fixed inset-0 bg-black/40 flex items-center justify-center">
        <div class="bg-white rounded-xl p-6 w-96">
          <h2 class="text-lg font-semibold mb-4">Freeze wallet</h2>
          <textarea
            [formControl]="reason"
            placeholder="Reason (required)"
            class="w-full border rounded p-2 mb-4"
            rows="3"
          ></textarea>
          <div class="flex gap-2 justify-end">
            <button (click)="dialogOpen.set(false)">Cancel</button>
            <button
              [disabled]="reason.invalid"
              (click)="submit()"
              class="bg-red-600 text-white px-4 py-2 rounded disabled:opacity-50"
            >Freeze</button>
          </div>
        </div>
      </div>
    }
  `,
})
export class FreezeWalletComponent {
  readonly walletId = input.required<string>();

  private readonly http = inject(HttpClient);
  protected readonly canFreeze = hasPermission('wallet:manage');
  protected readonly dialogOpen = signal(false);
  protected readonly reason = new FormControl('', [Validators.required, Validators.minLength(3)]);

  submit() {
    if (this.reason.invalid) return;
    this.http.post(`/v1/wallets/${this.walletId()}/freeze`, { reason: this.reason.value })
      .subscribe(() => this.dialogOpen.set(false));
  }
}
```

> **Transaction reversal is not a delete.** The backend reverses by posting
> **compensating ledger entries** (a new `REVERSAL` transaction linked via
> `reverses_id`), preserving the immutable ledger.

---

## 9. Health Panel & Observability Links

**What.** A dashboard home panel showing system health and deep links into
Grafana for metrics, rather than re-implementing charts.

**Code — health component.**

```typescript
// features/dashboard/health.component.ts
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { interval, switchMap, startWith } from 'rxjs';

interface HealthStatus { status: 'UP' | 'DEGRADED' | 'DOWN'; broker: string; db: string; }

@Component({
  selector: 'app-health',
  template: `
    @if (health(); as h) {
      <div class="bg-white rounded-xl border border-slate-200 p-4">
        <span [class]="h.status === 'UP' ? 'text-green-600' : 'text-red-600'">
          {{ h.status }}
        </span>
        <span class="text-slate-500 ml-4">DB: {{ h.db }} · Broker: {{ h.broker }}</span>
      </div>
    }
  `,
})
export class HealthComponent {
  private readonly http = inject(HttpClient);

  protected readonly health = toSignal(
    interval(30_000).pipe(
      startWith(0),
      switchMap(() => this.http.get<HealthStatus>('/v1/admin/health')),
    ),
  );
}
```

---

## 10. Build & Deployment

**What.** `ng build` produces a hashed static bundle deployed to **S3 +
CloudFront**; there is **no app server**. The console talks to the API origin
over HTTPS only.

**How.**
- `ng build --configuration production` → `dist/` with content-hashed assets.
- Upload to S3; CloudFront serves it; SPA fallback routes 404s to `index.html`.
- Runtime config (API base URL, Grafana URLs) via environment files
  (`environment.prod.ts`) per environment.
- Dev: `ng serve` with `proxy.conf.json` proxying `/v1/*` → `http://localhost:8080`.

```jsonc
// src/proxy.conf.json
{
  "/v1": {
    "target": "http://localhost:8080",
    "secure": false
  }
}
```

---

## 11. Testing Strategy (Console)

| Layer        | Tool                            | What it covers                                                |
|--------------|---------------------------------|---------------------------------------------------------------|
| Unit         | Jest + `TestBed`                | Auth service signal state, permission helpers, money formatting. |
| Component    | `TestBed` + `ComponentFixture`  | RBAC-gated elements hide without permission; reason input enforced. |
| Integration  | `HttpClientTestingModule`       | Search → detail → audited action → trail refresh happy path.  |
| E2E          | Playwright                      | Operator login → freeze wallet → audit row appears.           |

**Critical test — a freeze button is hidden without its permission.**

```typescript
// features/wallets/freeze-wallet.component.spec.ts
it('hides Freeze wallet when operator lacks wallet:manage', () => {
  TestBed.overrideProvider(AuthService, {
    useValue: { isAuthenticated: signal(true), perms: signal(new Set(['wallet:read'])) },
  });
  const fixture = TestBed.createComponent(FreezeWalletComponent);
  fixture.componentRef.setInput('walletId', 'w1');
  fixture.detectChanges();

  expect(fixture.nativeElement.querySelector('button')).toBeNull();
});

it('disables Freeze button when reason is empty', () => {
  TestBed.overrideProvider(AuthService, {
    useValue: { isAuthenticated: signal(true), perms: signal(new Set(['wallet:manage'])) },
  });
  const fixture = TestBed.createComponent(FreezeWalletComponent);
  fixture.componentRef.setInput('walletId', 'w1');
  fixture.detectChanges();

  fixture.nativeElement.querySelector('button').click();
  fixture.detectChanges();

  const confirmBtn = fixture.nativeElement.querySelector('button[disabled]');
  expect(confirmBtn).not.toBeNull(); // disabled with empty reason
});
```

---

## 12. Decision Summary

| Decision                     | Choice                                                       |
|------------------------------|--------------------------------------------------------------|
| Framework                    | Angular 22 + TypeScript (standalone components)             |
| State                        | Signals: `signal()`, `computed()`, `toSignal()`             |
| DI                           | `inject()` throughout (not constructor injection)            |
| Control flow                 | `@if`, `@for` (not structural directives)                   |
| HTTP                         | `HttpClient` + functional interceptors (`HttpInterceptorFn`) |
| Build / bundler              | Angular CLI (`ng build`) → static bundle                    |
| Hosting                      | S3 + CloudFront (no app server, outside k8s)                |
| Routing                      | Lazy `loadComponent` with `CanActivateFn` guards            |
| Access-token storage         | Signal in memory only                                       |
| Refresh-token storage        | `Secure; HttpOnly; SameSite` cookie (set by backend)        |
| Styling                      | Tailwind CSS 4, Material Symbols Outlined, Inter            |
| RBAC in UI                   | Mirror of server perms — UI hides, **server enforces**      |
| Operator actions             | Mandatory typed reason → audited (`AuditRecord`)            |
| Transaction reversal         | Compensating ledger entries, never a delete                 |
| Observability                | Links out to Grafana; no re-implemented charts              |
| Testing                      | Jest + TestBed + HttpClientTestingModule + Playwright       |

> **Bottom line:** the dashboard is a static, server-authoritative operator
> console built with Angular 22 patterns (signals, inject(), standalone). It
> mirrors RBAC for usability while the backend enforces it for real, makes every
> sensitive action carry an audited reason, and links out to the existing
> observability stack instead of duplicating it.

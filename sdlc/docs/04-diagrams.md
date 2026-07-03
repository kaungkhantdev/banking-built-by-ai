# Diagrams

> Digital Banking Platform · ERD + Mobile & Dashboard Flows
> Rendered images live in `diagrams/`. Mermaid source kept below each image.

To re-render after editing any `.mmd`:
```
npx -y @mermaid-js/mermaid-cli -i diagrams/src/erd.mmd -o diagrams/erd.png -b white -s 2
```

---

## A. Mobile App Flow

End-user journey through the React Native app — unlock, onboarding, KYC, wallets, and
the transfer flow with on-device idempotency.

![Mobile App Flow](diagrams/mobile-flow.png)

<details><summary>Mermaid source</summary>

```mermaid
flowchart TD
    START([App launch]) --> BIO{Biometric /<br/>PIN unlock}
    BIO -->|fail| BIO
    BIO -->|ok| AUTH{Logged in?}

    AUTH -->|no| REG[Register / Login]
    REG --> JWT[Receive JWT +<br/>refresh token<br/>stored in secure enclave]
    JWT --> KYCCHECK

    AUTH -->|yes| KYCCHECK{KYC verified?}

    KYCCHECK -->|no| KYC[Submit KYC:<br/>document + selfie capture]
    KYC --> KYCWAIT[Status PENDING<br/>push on decision]
    KYCWAIT --> HOME

    KYCCHECK -->|yes| HOME[Home: wallet list<br/>balances + history]

    HOME --> ACT{User action}
    ACT -->|view| HIST[Transaction history]
    ACT -->|transfer| TF[Enter amount + destination]

    TF --> IDEM[Generate on-device<br/>idempotency key]
    IDEM --> SEND[POST /v1/transfers]
    SEND --> RESP{Result}
    RESP -->|network fail| RETRY[Retry with same key<br/>no double-charge]
    RETRY --> SEND
    RESP -->|422 rejected| ERR[Show reason<br/>frozen / unverified / low balance]
    RESP -->|201 done| OKK[Show success]
    OKK --> PUSH[Push: TransferCompleted]
    PUSH --> HOME
    ERR --> HOME
    HIST --> HOME

    HOME --> LOGOUT[Logout:<br/>clear tokens + cached PII]
    LOGOUT --> START
```
</details>

---

## B. Admin Dashboard Flow

Operator journey through the React console — RBAC-gated search, entity views,
and audited operator actions.

![Admin Dashboard Flow](diagrams/dashboard-flow.png)

<details><summary>Mermaid source</summary>

```mermaid
flowchart TD
    START([Operator opens dashboard]) --> LOGIN[Login - JWT]
    LOGIN --> RBAC{RBAC: resolve<br/>roles + permissions}
    RBAC -->|no access| DENY[403 Forbidden<br/>audited]
    RBAC -->|ok| HOME[Dashboard home<br/>health panel + search]

    HOME --> SEARCH[Search customers / wallets /<br/>transactions / KYC cases]
    SEARCH --> ENTITY[Open entity detail]
    ENTITY --> VIEW{View}
    VIEW -->|audit| TRAIL[Per-entity audit trail]
    VIEW -->|action| PERM{Has required<br/>permission?}

    PERM -->|no| DENY
    PERM -->|yes| OP{Operator action}

    OP -->|account| SUS[Suspend / close account]
    OP -->|wallet| FRZ[Freeze / unfreeze wallet]
    OP -->|transaction| REV[Reverse transaction<br/>compensating entries]
    OP -->|KYC| OVR[Compliance override decision]

    SUS --> CALL[Call /v1 API]
    FRZ --> CALL
    REV --> CALL
    OVR --> CALL

    CALL --> AUDIT[Action audited:<br/>actor + reason + before/after]
    AUDIT --> EVENT[Domain event emitted]
    EVENT --> HOME
    TRAIL --> HOME

    HOME --> METRICS[View dashboards / metrics<br/>link to Grafana]
    METRICS --> HOME
```
</details>

---

## C. Entity Relationship Diagram (ERD)

![Entity Relationship Diagram](diagrams/erd.png)

<details><summary>Mermaid source</summary>

```mermaid
erDiagram
    CUSTOMERS ||--o{ WALLETS : owns
    CUSTOMERS ||--o{ KYC_CASES : "has"
    CUSTOMERS ||--o{ USER_ROLES : "assigned"
    CUSTOMERS ||--o{ REFRESH_TOKENS : "holds"

    WALLETS ||--o{ LEDGER_ENTRIES : "records"
    WALLETS ||--o{ TRANSFERS : "source/dest"

    TRANSACTIONS ||--|{ LEDGER_ENTRIES : "posts"
    TRANSACTIONS ||--o| TRANSFERS : "settles"
    TRANSACTIONS ||--o| TRANSACTIONS : "reverses"

    ROLES ||--o{ ROLE_PERMISSIONS : "grants"
    PERMISSIONS ||--o{ ROLE_PERMISSIONS : "in"
    ROLES ||--o{ USER_ROLES : "mapped"

    CUSTOMERS {
        uuid id PK
        citext email UK
        text phone UK
        enum status "PENDING|ACTIVE|SUSPENDED|CLOSED"
        enum kyc_status "NOT_STARTED|PENDING|VERIFIED|REJECTED"
        timestamptz created_at
    }
    WALLETS {
        uuid id PK
        uuid customer_id FK
        char currency "ISO 4217"
        enum status "ACTIVE|FROZEN|CLOSED"
        timestamptz created_at
    }
    TRANSACTIONS {
        uuid id PK
        enum type "CREDIT|DEBIT|HOLD|RELEASE|REVERSAL"
        enum status "PENDING|POSTED|FAILED|REVERSED"
        text idempotency_key UK
        uuid reverses_id FK
        jsonb metadata
        timestamptz created_at
    }
    LEDGER_ENTRIES {
        uuid id PK
        uuid transaction_id FK
        uuid wallet_id FK
        enum direction "DEBIT|CREDIT"
        numeric amount "NUMERIC(20,4)"
        timestamptz created_at
    }
    TRANSFERS {
        uuid id PK
        uuid source_wallet FK
        uuid dest_wallet FK
        numeric amount
        enum status "PENDING|COMPLETED|FAILED"
        uuid transaction_id FK
        timestamptz created_at
    }
    KYC_CASES {
        uuid id PK
        uuid customer_id FK
        enum status "PENDING|VERIFIED|REJECTED"
        text vendor_ref
        uuid decided_by
        timestamptz decided_at
    }
    AUDIT_RECORDS {
        uuid id PK
        uuid actor_id
        text action
        text entity_type
        uuid entity_id
        jsonb before
        jsonb after
        text trace_id
        timestamptz created_at
    }
    ROLES {
        uuid id PK
        text name UK
    }
    PERMISSIONS {
        uuid id PK
        text name UK
    }
    ROLE_PERMISSIONS {
        uuid role_id FK
        uuid permission_id FK
    }
    USER_ROLES {
        uuid user_id FK
        uuid role_id FK
    }
    REFRESH_TOKENS {
        uuid id PK
        uuid user_id FK
        text token_hash
        enum status "ACTIVE|ROTATED|REVOKED"
        uuid rotated_from
        timestamptz expires_at
    }
    OUTBOX {
        uuid id PK
        text aggregate_type
        text event_type
        jsonb payload
        boolean published
        timestamptz created_at
    }
```
</details>

> `AUDIT_RECORDS` and `OUTBOX` are intentionally not foreign-keyed to business
> tables — audit is append-only across all entities, and the outbox is a generic
> event buffer. Both reference entities by id/type rather than hard FKs.

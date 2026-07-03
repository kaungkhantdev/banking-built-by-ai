# Deployment & Cloud Infrastructure

> Digital Banking Platform · Monolith Architecture + Cloud Setup
> Status: Baseline · Owner: Architect + Platform / DevOps

---

## 8.1 Purpose

Describe, concretely, how the **modular-monolith** backend (Java / Spring Boot),
the **React** dashboard, and the **React Native** mobile app are packaged, run, and
scaled in the cloud — including the event-driven pieces (outbox + broker), the
database, observability, networking, environments, and a realistic launch-size
footprint. This is the bridge between the design (what) and operations (where it
runs).

---

## 8.2 The Monolith, Concretely

One deployable Spring Boot application, internally split into modules. It is a
**modular monolith**: one process, one codebase, clean internal boundaries — not
many services.

```
            ┌──────────────────────────────────────────────┐
            │        Spring Boot application (1 image)       │
            │                                                │
            │  HTTP  ┌──────────────────────────────────┐    │
            │  ────▶ │ Web layer (REST /v1, Spring MVC) │    │
            │        └───────────────┬──────────────────┘    │
            │   ┌────────────────────┼────────────────────┐  │
            │   ▼          ▼         ▼         ▼         ▼  │
            │ [auth]  [accounts] [wallets] [ledger] [transfers]
            │ [kyc]   [audit]    [events/outbox]   [shared]  │
            │   │          │         │         │         │   │
            │   └──────────┴────┬────┴─────────┴─────────┘   │
            │                   ▼ (JPA / JDBC)               │
            │            money core: single TX boundary       │
            │                                                │
            │  [@Scheduled outbox relay]  ── publishes ──▶    │ ─▶ broker
            │  [@RabbitListener consumers] ◀── consume ──     │ ◀─ broker
            │  [Actuator /metrics /health]                   │
            └────────────────────────────────────────────────┘
```

Key properties:
- **Stateless app:** no session state in memory; all state lives in the
  database. This is what lets us run **N identical replicas** behind a load
  balancer.
- **One money database, one transaction boundary:** `ledger` + `transfers` +
  `outbox` commit together (`@Transactional`).
- **Event-driven inside the app:** the relay and consumers run on background
  threads; the broker carries events out and back (see §3.7).

---

## 8.3 Deployable Units (what actually gets shipped)

| Unit                | Built with            | Packaged as                    | Runs as                         |
|---------------------|-----------------------|--------------------------------|---------------------------------|
| Backend API         | Spring Boot (JAR)     | Docker image (`backend:<ver>`) | N replicas behind a load balancer |
| Admin Dashboard     | React (static build)  | Static files (Nginx/CDN)       | Served by CDN or an Nginx container |
| Mobile App          | React Native          | `.ipa` / `.apk` (app stores)   | On user devices                 |
| Outbox relay        | (part of backend)     | Same backend image             | 1 replica (or leader-elected)   |
| Event consumers     | (part of backend)     | Same backend image             | Inside backend replicas         |

> The dashboard is just static files talking to the API — it needs no server of
> its own beyond a CDN/Nginx. The mobile app ships to the App Store / Play Store.
> Only the **backend** needs orchestration.

---

## 8.4 Cloud Architecture (target)

```
                         Internet
                            │
                   ┌────────┴─────────┐
                   │   CDN / WAF       │  (TLS, static dashboard, edge cache)
                   └────────┬─────────┘
                            ▼
                   ┌──────────────────┐
                   │   API Gateway /   │  TLS, JWT check, rate limit,
                   │   Load Balancer   │  routing, trace-id
                   └────────┬─────────┘
            ┌───────────────┼───────────────┐
            ▼               ▼               ▼
      ┌──────────┐   ┌──────────┐   ┌──────────┐
      │ backend  │   │ backend  │   │ backend  │   N replicas of ONE image
      │ replica  │   │ replica  │   │ replica  │   (auto-scaled)
      └────┬─────┘   └────┬─────┘   └────┬─────┘
           └──────────────┼──────────────┘
              ┌───────────┼───────────┐
              ▼           ▼           ▼
      ┌────────────┐ ┌──────────┐ ┌────────────┐
      │ PostgreSQL │ │ RabbitMQ │ │  Object    │
      │ (primary + │ │ (broker) │ │  storage   │  (KYC docs, exports)
      │  replica)  │ └──────────┘ └────────────┘
      └────────────┘
              │
              ▼
      ┌────────────────────────────┐
      │ Prometheus + Grafana +      │  metrics, dashboards, tracing
      │ tracing backend             │
      └────────────────────────────┘
```

### Components
- **CDN / WAF:** terminates TLS at the edge, serves the React static bundle,
  filters malicious traffic, caches static assets.
- **API Gateway / Load Balancer:** single ingress to the backend; validates JWT,
  applies rate limiting, injects `trace_id`, load-balances across replicas.
- **Backend replicas:** N copies of the one Spring Boot image; horizontally
  auto-scaled.
- **PostgreSQL:** managed, with a primary for writes, a read replica, automated
  backups, and **point-in-time recovery (PITR)**. This is the money system of
  record.
- **RabbitMQ:** the event broker (lightweight; see sizing).
- **Object storage:** KYC documents, signed audit exports (e.g. S3-compatible).
- **Observability stack:** Prometheus scrapes `/metrics`; Grafana dashboards;
  a tracing backend collects spans.

---

## 8.5 Orchestration

### Dev / small staging — Docker Compose
A single `docker-compose.yml` brings up the whole stack with one command:
`backend`, `postgres`, `rabbitmq`, `prometheus`, `grafana`. Ideal for local
development and a small staging box.

### Production — Kubernetes (or a managed container service)
- **Deployment:** declares the desired replica count of the backend image; the
  orchestrator keeps that many alive and replaces failed ones.
- **Service + Ingress:** load-balances across replicas; TLS at the edge.
- **Liveness/Readiness probes:** wired to Actuator `/health`; a hung replica is
  restarted automatically; a not-ready replica receives no traffic.
- **Horizontal Pod Autoscaler (HPA):** add replicas when CPU/latency crosses a
  threshold (e.g. a payment surge), remove them after — pay for capacity only
  when used.
- **Rolling updates:** replace replicas one at a time → zero-downtime deploys
  and instant rollback to the previous image.
- **Secrets/config:** DB credentials, JWT signing keys, vendor API keys in a
  secrets manager — never in the image.

### Two stateful constraints (important)
- **Database:** one primary writer (plus read replicas / failover). You do not
  run N independent writers — state has a single home.
- **Outbox relay:** exactly **one active publisher** (single replica or
  leader-elected) so events aren't double-published. Consumers are idempotent,
  so a rare duplicate is survivable, but one relay is cleaner.

---

## 8.6 Environments & Promotion

| Env       | Purpose                          | Infra                                   |
|-----------|----------------------------------|-----------------------------------------|
| Local     | Dev inner loop                   | Docker Compose on the developer machine |
| CI        | Automated tests on every push    | Ephemeral containers in the pipeline    |
| Staging   | E2E, perf, UAT, security         | Prod-like, smaller replica counts, no real PII |
| Prod      | Live                             | Full HA: multi-AZ, autoscaling, backups |

**Promotion:** the same immutable image (`backend:<ver>`) is built once, tested
in CI, deployed to staging, then promoted to prod — identical artifact, only
config/secrets differ per environment.

---

## 8.7 CI/CD Pipeline

```
git push
   │
   ▼
[CI]  build JAR → unit + integration tests → contract tests → SAST/SCA
   │   → build Docker image → push to registry (tag = commit/version)
   ▼
[CD]  deploy image to STAGING → run E2E + smoke
   │   → manual/auto gate
   ▼
      promote SAME image to PROD → rolling update → health-gated → done
                                      │ on failure
                                      ▼ automatic rollback to previous image
```

- One image, built once, promoted across environments (no rebuild per env).
- Health-gated rollouts; rollback is just "redeploy previous tag."

---

## 8.8 Launch-Size Footprint (realistic, not a "big server")

A modular monolith + event-driven runs on a modest footprint:

| Component        | Launch sizing                         | Notes                                  |
|------------------|---------------------------------------|----------------------------------------|
| Backend replicas | 2–3 × (1 vCPU, 1–2 GB) on a 2–4 vCPU node | Start at 2 for HA; autoscale up        |
| PostgreSQL       | Small managed tier (2 vCPU, 4–8 GB)   | Primary + 1 read replica, PITR on      |
| RabbitMQ         | 512 MB–1 GB                            | Lightweight; can co-locate at first    |
| Prometheus+Grafana | Small (1 vCPU, 1–2 GB)              | Scales with retention                  |
| Object storage   | Pay-per-use                           | KYC docs, exports                      |

> This fits comfortably on a single small cloud VM (Docker Compose) for early
> staging, and a small Kubernetes node pool for production. You **scale
> horizontally in cheap increments** — add a backend replica when traffic
> demands — rather than buying a big server upfront. Microservices, by contrast,
> would multiply JVM overhead and infra (per-service DBs, discovery, mesh) and
> cost **more** for the same load.

---

## 8.9 Data Durability & Disaster Recovery

> The single most important rule: **a replica is not a backup.** A replica keeps
> the system *available*; backups let you *recover*. Money data needs both, in
> layers, because no single mechanism covers every failure.

### 8.9.1 Don't self-host the production database in Docker

Running PostgreSQL in a plain Docker container on one host is fine for **local
dev** and small staging — but **not for production money data**. If that
container or its host dies and the data lived only there, it is gone.

For production, use a **managed PostgreSQL** service (AWS RDS, Google Cloud SQL,
Azure Database for PostgreSQL, DigitalOcean Managed Databases, etc.). The
provider runs the database and the durability machinery (replication, continuous
backup, PITR, cross-region copies); the app just connects to it.

> **The split:** Docker/Kubernetes orchestrate the **stateless app**. The
> **stateful database** lives outside that, as a managed service. Never put the
> only copy of money data inside an ephemeral container.

### 8.9.2 Why "primary + replica" alone is not enough

A replica protects against exactly **one** failure: the primary crashing. It
does **not** protect against the failures that destroy data, because a replica
faithfully copies whatever the primary does — including mistakes:

| Failure                                   | Does a replica save you? |
|-------------------------------------------|--------------------------|
| Primary server crashes                    | ✅ Yes — promote replica |
| Bad deploy / bug DELETEs or corrupts rows | ❌ No — replicated instantly |
| Accidental `DROP TABLE`                   | ❌ No — replicated instantly |
| Ransomware / account compromise           | ❌ No — hits both         |
| Whole datacenter / region outage          | ❌ No (if same location)  |

A replica is for **availability**; it is **not** a recovery tool. This is
exactly why "what if both are down?" is the right question — replicas alone do
not answer it.

### 8.9.3 The layered protection (defense in depth)

Money data is protected by a **stack** of independent mechanisms, not one:

1. **Replica (high availability).** If the primary fails, the replica is
   promoted in seconds-to-minutes. Place it in a **different availability zone**
   so one datacenter failure can't take both.

2. **Continuous backup + Point-In-Time Recovery (PITR) — the real safety net.**
   The database continuously ships its write-ahead log (WAL) to **separate
   storage** (e.g. object storage / S3). This lets you restore to **any moment**
   — e.g. "the exact state at 14:32:05, one second before the bad `DELETE`."
   Even if both primary and replica are lost or corrupted, the database is
   rebuilt from these backups. This is what delivers the **RPO ≤ 5 min**
   guarantee: at most ~5 minutes of data is ever at risk.

3. **Periodic snapshots.** Automated daily full backups, retained for
   weeks-to-months, stored separately from the live database.

4. **Offsite / cross-region copies.** Backups copied to a **different
   geographic region**. If an entire region is lost, the data still exists
   elsewhere.

### 8.9.4 "Both database servers down" — what actually happens

| Scenario                                  | Outcome                                                            |
|-------------------------------------------|-------------------------------------------------------------------|
| Primary crashes                           | Replica promoted → keep running. **No data loss.**               |
| Primary **and** replica both crash        | App down briefly, but **data is safe in backups**. Restore from PITR → back online with ≤ RPO loss. |
| Someone runs `DROP TABLE` / bad migration | Replica copied it too — but **PITR restores** to the instant before. |
| Whole region gone                         | Restore from the **cross-region backup** in another region.       |
| **Backups also lost**                     | ← the only true "lose everything." Prevented by storing backups separately, cross-region, with retention + tested restores. |

> So: primary **and** replica both down does **not** mean data lost — the
> backups live in separate storage (and another region), independent of both
> database instances. You restore from them and lose at most the RPO window,
> not everything.

### 8.9.5 The 3-2-1 rule (industry standard)

> Keep **3** copies of the data, on **2** different storage types, with **1**
> copy offsite.

Our mapping: **primary (1)** + **replica (2)** + **backup in object storage in
another region (3, offsite)**. That is precisely why "both database servers
down" never equals "data lost."

### 8.9.6 A backup you have never restored is not a backup

Backups silently fail (wrong config, corrupted archive, missing tables). The
only proof they work is a **restore drill**: periodically restore a backup into
a clean environment and verify the data — including that the **ledger invariants
still hold** (debits = credits, balances reconcile). This validates both the
**RPO ≤ 5 min** and the **RTO ≤ 1 hr** (time to be back online) targets.

### 8.9.7 Design-level recovery path (specific to this platform)

Beyond database backups, the **data model itself** adds a recovery path:

- The **ledger is append-only and double-entry** — it is a complete,
  reconstructable history of every money movement.
- There is an **immutable audit trail** and an **event stream** of everything
  that happened.

So even in a catastrophic partial-data scenario, balances can be
**reconciled/replayed** from the ledger, audit, and events — a recovery layer on
top of the database backups. This is a deliberate property of the money model,
not an accident.

### 8.9.8 Other reliability measures

- **High availability:** app replicas and DB standby spread across availability
  zones; broker persistence enabled so queued events survive a broker restart.
- **Graceful degradation:** if a consumer or external vendor is down, events
  queue and retry; the money path keeps working.
- **Dead-letter queue:** poison events are parked and replayable.
- **Backups of the broker/object storage:** KYC documents and signed audit
  exports in object storage are themselves versioned and replicated.

### 8.9.9 Recovery targets (summary)

| Target | Value      | Meaning                                                  |
|--------|------------|----------------------------------------------------------|
| RPO    | ≤ 5 min    | Maximum data loss window (covered by continuous WAL/PITR)|
| RTO    | ≤ 1 hr     | Maximum time to restore core services                    |
| Backup retention | weeks–months | How far back you can restore                    |
| Restore drill    | periodic     | Proves backups actually work                    |
| Copies (3-2-1)   | 3 / 2 / 1    | 3 copies, 2 media, 1 offsite/cross-region       |

---

## 8.10 Security of the Infrastructure

- TLS everywhere (edge and service-to-service).
- Secrets in a managed vault; signing keys rotated.
- Least-privilege network policies: only the backend reaches the DB/broker; the
  DB is never public.
- PII encrypted at rest (DB + object storage); KYC docs access-controlled.
- WAF + rate limiting at the edge; gateway enforces JWT before anything reaches
  a service.

---

## 8.11 Scaling Path (when you outgrow the monolith)

You do **not** start with microservices, but the design leaves the door open:

1. **First:** scale the monolith horizontally (more replicas) — handles a large
   range of growth cheaply.
2. **Then:** offload reads to DB read replicas; cache hot reads.
3. **Later, only if a specific module needs independent scale or ownership:**
   extract that one module into its own service. Because modules already
   communicate via events and clean interfaces, extraction is low-effort and
   incremental — pull out `kyc` or `notifications` first (they're already
   event-fed), leave the money core intact.

> Extract a service for a concrete reason (independent scale, separate team,
> compliance isolation) — not by default. The monolith carries you a long way.

---

## 8.12 Reference Implementation on AWS (EKS + GitOps)

### 8.12.1 Components → AWS services

| Need                     | AWS service                  |
|--------------------------|------------------------------|
| Kubernetes cluster       | **EKS** (managed Kubernetes) |
| Database (PostgreSQL)    | **RDS PostgreSQL** (Multi-AZ)|
| Message broker           | **Amazon MQ** (RabbitMQ)     |
| Object storage           | **S3** (KYC docs, exports)   |
| Container registry       | **ECR**                      |
| Load balancer / ingress  | **ALB** (AWS LB Controller)  |
| Dashboard (React)        | **S3 + CloudFront**          |
| TLS / CDN / firewall     | **CloudFront + WAF + ACM**   |
| Secrets                  | **Secrets Manager**          |
| Metrics / logs           | **CloudWatch** + Prometheus/Grafana |

**Runs inside the cluster (GitOps-managed):** backend monolith
(Deployment + Service + HPA), outbox relay (1 replica), ingress, Prometheus +
Grafana, the GitOps controller.
**Stays outside as managed services:** RDS, Amazon MQ, S3, Secrets Manager — the
database is **never** run inside Kubernetes (same durability reasoning as §8.9).

### 8.12.2 Why EKS (not ECS) here

GitOps tooling (Argo CD, Flux) is **Kubernetes-native**. Choosing GitOps is the
reason to use **EKS** rather than ECS Fargate — the cluster's entire desired
state is declarative YAML a controller can reconcile from Git.

### 8.12.3 What GitOps is

Git is the **single source of truth** for what runs in the cluster:

1. Desired state (Deployments, replicas, config) lives as YAML in a Git repo.
2. A controller in the cluster (**Argo CD** or **Flux**) watches that repo.
3. On a Git change, the controller makes the cluster match Git automatically.
4. Manual cluster drift is detected and reverted back to Git.

To change production you open a **pull request**; merge → the cluster updates
itself. Git history is the deployment history; **rollback = `git revert`**. Every
production change is a reviewed commit — a compliance win for a regulated system.

### 8.12.4 The two-repo flow

```
[App repo]  code → CI builds image → push ECR:backend:v1.4
                                          │
                                          ▼ CI bumps image tag (commit/PR)
[Config repo]  k8s YAML (image: v1.4, replicas, ingress, HPA)
                                          │ watched by
                                          ▼
                                    Argo CD (in EKS)
                                          │ syncs
                                          ▼
                          rolling update to v1.4 → Synced / Healthy
                                          │ on failure
                                          ▼ auto-rollback
```

- **App repo:** Spring Boot / React / React Native source. CI builds and pushes the
  image to ECR.
- **Config repo (the GitOps repo):** Kubernetes manifests Argo CD watches.
  Promotion to prod = a commit bumping the image tag in the prod overlay.

### 8.12.5 Tooling

- **Controller:** Argo CD (has a UI showing sync status/diffs — friendlier to
  learn) or Flux (lighter, CLI/CRD-driven).
- **Manifests:** Kustomize (base + `dev`/`staging`/`prod` overlays) or Helm.
  Kustomize is simpler to start.
- **Environments:** one Argo CD `Application` per env; promote by bumping the
  image tag up the overlays via PR.
- **Secrets:** never commit raw secrets. Use **External Secrets Operator**
  (pulls from AWS Secrets Manager into the cluster) or **Sealed Secrets**
  (encrypted, safe to commit).

### 8.12.6 Picture

```
Git (config repo) ──watched by──▶ Argo CD ──┐ syncs
                                            ▼
  ┌──────────────── EKS (Kubernetes) ────────────────┐
  │  Ingress (ALB) → backend Deployment (HPA)         │
  │  outbox relay (1 replica)                         │
  │  Prometheus + Grafana                             │
  │  External Secrets Operator ──pulls──▶ Secrets Mgr │
  └───────────────┬─────────────┬─────────────────────┘
                  ▼             ▼             ▼
              RDS Postgres   Amazon MQ        S3
              (managed)      (managed)     (managed)
```

### 8.12.7 Adoption order (don't learn it all at once)

1. Monolith on EKS with plain manifests — get cluster basics working.
2. Add Argo CD pointing at the config repo.
3. Add Kustomize overlays per environment.
4. Add External Secrets for credentials.

> Caveat: GitOps + EKS is powerful (auditable, self-healing, declarative) but is
> a real learning curve. Sequence it after the cluster basics are comfortable —
> don't adopt Kubernetes, Argo CD, and event-driven design all in the same week.

---

## 8.13 Summary

| Decision               | Choice                                                       |
|------------------------|--------------------------------------------------------------|
| Backend packaging      | One Docker image (Spring Boot), N stateless replicas         |
| Dashboard hosting      | React static build on S3 + CloudFront                        |
| Mobile distribution    | React Native apps via App Store / Play Store                 |
| Orchestration (dev)    | Docker Compose                                               |
| Orchestration (prod)   | **EKS** (Kubernetes): Deployment + Service + HPA + probes    |
| Deployment model       | **GitOps** (Argo CD): Git is the source of truth             |
| Database               | RDS PostgreSQL, Multi-AZ, PITR (managed, outside k8s)        |
| Broker                 | Amazon MQ (RabbitMQ), managed                                |
| Event relay            | Single active replica (leader-elected)                       |
| Secrets                | Secrets Manager via External Secrets Operator                |
| Footprint              | Small node pool; scale horizontally on demand                |
| Microservices          | Deferred — extract a module only when a concrete need arises |

# Contents

| # | Phase     | Document                               | Purpose                                                            |
|---|-----------|----------------------------------------|-------------------------------------------------------------------|
| 1 | Planning  | `docs/01-planning.md`                  | Vision, scope, stakeholders, risk, cost, schedule, feasibility.   |
| 2 | Analysis  | `docs/02-analysis.md`                  | Functional + non-functional requirements, use cases, data needs.  |
| 3 | Design    | `docs/03-design.md`                    | Architecture, services, data models, APIs, security, observability.|
| 3 | Diagrams  | `docs/04-diagrams.md`                  | ER diagram + mobile and dashboard flow diagrams (rendered images). |
| 4 | Implement | `docs/05-implementation-plan.md`       | Backlog, epics, milestones, sprint-by-sprint delivery plan.       |
| 5 | Testing   | `docs/06-testing-strategy.md`          | Test pyramid, test cases, NFR tests, CI gates, exit criteria.     |
| 7 | Tech Stack| `docs/07-tech-stack.md`                | Spring Boot / React / React Native; event-driven vs microservices.|
| 8 | Infra     | `docs/08-deployment-infra.md`          | Monolith deployment, cloud architecture, orchestration, scaling.  |
| 9 | Backend   | `docs/09-backend-api.md`               | Spring Boot feature docs + code: auth, RBAC, ledger, outbox, etc.  |
| 9 | Backend   | `apps/api/`                            | Spring Boot source + per-feature docs and FR/NFR specs (`apps/api/docs/`). |
| 10| Mobile    | `docs/10-mobile-app.md`                | React Native feature docs + code: secure auth, KYC, transfers, push.|
| 10| Mobile    | `apps/mobile/`                         | React Native source + per-feature docs and FR/NFR specs (`apps/mobile/docs/`). |
| 11| Dashboard | `docs/11-admin-dashboard.md`           | React feature docs + code: RBAC UI, search, operator actions.     |
| 11| Dashboard | `apps/web/`                            | React (Vite) source + per-feature docs and FR/NFR specs (`apps/web/docs/`). |
| 12| GitOps    | `docs/12-gitops-deployment.md`         | EKS + Argo CD: Dockerfile, manifests, Kustomize, CI/CD, secrets.   |

## Repository layout

```text
banking-sdlc/
├── README.md                          # this index
├── Digital-Banking-Platform-SDLC.pdf  # the compiled deliverable
├── docs/                              # human-readable SDLC chapters (Markdown)
│   ├── 01-planning.md … 12-gitops-deployment.md
│   └── diagrams/                      # rendered .png / .svg
│       └── src/                       # mermaid (.mmd) sources
├── apps/                             # implemented source for each deliverable
│   ├── api/                          # Spring Boot modular monolith (backend)
│   │   ├── src/main/java/com/bank/    # feature-first Java source + tests
│   │   ├── docs/                      # backend per-feature docs + FR/NFR specs
│   │   └── pom.xml, mvnw              # Maven toolchain
│   ├── mobile/                       # React Native + TypeScript client
│   │   ├── src/{app,lib,features,ui}/ # feature-first app code + tests
│   │   ├── docs/                      # mobile per-feature docs + FR/NFR specs
│   │   └── package.json, tsconfig.json
│   └── web/                          # React (Vite) operator console
│       ├── src/{app,lib,features,ui}/ # feature-first app code + tests
│       ├── docs/                      # web per-feature docs + FR/NFR specs
│       └── package.json, tsconfig.json
└── tools/                            # build machinery (not for reading)
    ├── build.sh                       # one-command build
    ├── build_pdf.py                   # markdown → self-contained HTML
    ├── render_pdf.js                  # HTML → PDF with page-number footer
    └── fonts/DMSans-VF.ttf            # embedded font
```

**To rebuild the PDF:** `tools/build.sh` (renders diagrams, embeds them, prints
the PDF to the repo root).

## Product at a glance

**Deliverables**
- Backend API (core-banking services + gateway)
- Customer mobile app (iOS + Android)
- Admin dashboard (operator console)

**Core features**
- Customer accounts
- Wallets
- Transactions
- Transfers
- KYC (Know Your Customer)
- Audit logs
- Admin dashboard
- RBAC (Role-Based Access Control)

**Enterprise features**
- JWT + Refresh Token authentication
- Multi-role system
- Audit Trail
- Event-driven architecture
- API Gateway
- Rate Limiting
- Prometheus metrics
- Grafana dashboards
- Distributed tracing

## How to read this

Start with Planning to understand *why* and *what for*, move to Analysis for
*what exactly*, Design for *how*, then Implementation for *how it ships* and
Testing for *how it's proven*. Each document is self-contained but
cross-references the others.

Rating target: enterprise / regulated-fintech grade.

# GitOps Deployment on AWS EKS

> Digital Banking Platform · Continuous Delivery with Argo CD
> Status: Baseline · Owner: Platform / DevOps

This document describes how the modular-monolith backend, the supporting
event-relay process, and the surrounding platform are built into images,
declared as Kubernetes manifests, and continuously reconciled onto **AWS EKS**
using a **GitOps** workflow driven by **Argo CD**. It complements the cloud
infrastructure document (file 08) and the backend API document (file 09).

For each piece we cover **what** it is, **why** it is designed this way
(reasoning and trade-offs), **how** it works, and the **real config/code** that
implements it.

---

## 1. Overview & GitOps Principles

**What.** GitOps is an operating model where the *desired state* of the whole
runtime system is declared in Git, and an in-cluster controller (Argo CD)
continuously reconciles the live cluster to match Git. You never run
`kubectl apply` against production by hand; instead you open a pull request, get
it reviewed, merge it, and the controller rolls the change out.

**Why.** A digital banking platform needs auditability, reproducibility, and
safe rollback. Git already gives us reviewed change history, signed commits,
and `git revert`. By making Git the single source of truth we get:

- **Auditability** — every production change is a reviewed, attributable commit.
- **Reproducibility** — the cluster can be rebuilt from the repo at any SHA.
- **Drift correction** — if someone hand-edits a live object, the controller
  reverts it back to the declared state (self-heal).
- **Trivial rollback** — `git revert` of the offending commit returns the system
  to a known-good state through the same reviewed path.

**How — the two-repo model.** We separate *application code* from *deployment
state* so that an automated tag bump never triggers a code review and a code
change never silently mutates production:

```
┌─────────────────┐    build & test     ┌──────────────────────┐
│  app repo        │ ───────────────────▶│  ECR (image registry) │
│  (Java source)   │   push image:<sha>  │  backend:<git-sha>    │
└─────────────────┘                      └──────────┬───────────┘
        │  CI commits new tag                       │
        ▼                                           │
┌─────────────────┐    Argo CD syncs      ┌─────────▼───────────┐
│  config repo     │ ───────────────────▶ │  AWS EKS cluster     │
│  (k8s manifests) │   reconcile to Git    │  Deployments/Svcs    │
└─────────────────┘                       └─────────────────────┘
```

1. A merge to the **app repo** triggers CI: build the JAR, run tests, build the
   Docker image, push it to **Amazon ECR** tagged with the immutable git SHA.
2. CI then opens (or commits) a tag bump in the **config repo** — changing the
   image tag in the environment overlay.
3. **Argo CD**, running inside EKS, notices the config repo changed and syncs
   the cluster to the new desired state.

**Why EKS over ECS for GitOps.** ECS is a fine container runtime, but GitOps
tooling (Argo CD, Flux, External Secrets Operator, Prometheus Operator) is built
around the **Kubernetes declarative API**. Kubernetes gives us a single uniform
object model that Argo CD can diff and reconcile; ECS task definitions and
services are managed through AWS APIs that Argo cannot natively reconcile. EKS
also gives us portable manifests, mature autoscaling primitives (HPA), and a
rich ecosystem — at the cost of more moving parts than ECS. For a regulated
platform where reconciliation, drift detection, and a uniform control plane
matter, that trade-off is worth it.

> Note: stateful services (RDS PostgreSQL, Amazon MQ / RabbitMQ, S3, Secrets
> Manager) stay **outside** the cluster as managed AWS services. Kubernetes runs
> only the **stateless** backend replicas and the single event relay. The
> database is **never** run inside Kubernetes.

---

## 2. Containerizing the Backend

**What.** A single Docker image for the Spring Boot 3 modular monolith, built
with a multi-stage Dockerfile and run as N stateless replicas.

**Why a multi-stage build.** The build stage needs the full JDK and Maven; the
runtime needs only a slim JRE. Multi-stage keeps the final image small (faster
pulls, smaller attack surface) and ensures build tooling never ships to prod.
We run as a **non-root** user (defense in depth — a compromised process cannot
write outside its sandbox or escalate easily) and bake in a `HEALTHCHECK` that
hits Actuator so the container runtime and orchestrator agree on liveness.

**How.** Stage 1 resolves dependencies (cached layer) then builds the JAR.
Stage 2 copies only the JAR onto a distroless-style JRE base, drops privileges,
and exposes the Actuator port.

```dockerfile
# ---- Stage 1: build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Cache dependencies first for faster incremental builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Build the application JAR
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Stage 2: runtime ----
FROM eclipse-temurin:17-jre-jammy AS runtime

# Create an unprivileged user
RUN groupadd --system --gid 10001 app \
 && useradd  --system --uid 10001 --gid app --no-create-home app

WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar

# Drop root
USER 10001:10001
EXPOSE 8080

# Container-level health check against Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/liveness || exit 1

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
```

**`.dockerignore`** keeps the build context tiny and prevents secrets or local
cruft from leaking into image layers:

```gitignore
target/
.git/
.gitignore
.idea/
*.iml
*.md
.env
.env.*
docker-compose*.yml
**/node_modules
**/.DS_Store
k8s/
README*
```

---

## 3. CI Pipeline (GitHub Actions)

**What.** A workflow in the **app repo** that builds, tests, pushes the image to
ECR tagged with the git SHA, and then bumps that tag in the **config repo**.

**Why tag with the git SHA.** Immutable, content-addressable tags make every
deploy traceable to an exact commit and prevent the "mutable `latest`"
anti-pattern where two clusters running `:latest` are actually different. The
config repo edit is the *only* thing that triggers a deploy, so build and deploy
stay decoupled and the deploy is itself a reviewable Git change.

**How.** OIDC federation lets GitHub assume an AWS role with no long-lived keys.
After push, a small step checks out the config repo and rewrites the image tag
in the prod overlay via `kustomize edit set image`, then commits it. (For
stricter control, replace the direct commit with a PR — shown in the comment.)

```yaml
# .github/workflows/ci-cd.yml  (app repo)
name: ci-cd
on:
  push:
    branches: [main]

permissions:
  id-token: write      # required for AWS OIDC
  contents: read

env:
  AWS_REGION: eu-west-1
  ECR_REPOSITORY: banking/backend

jobs:
  build-test-push:
    runs-on: ubuntu-latest
    outputs:
      image_tag: ${{ steps.meta.outputs.sha }}
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven

      - name: Build & unit test
        run: mvn -B verify

      - name: Compute short SHA
        id: meta
        run: echo "sha=${GITHUB_SHA::12}" >> "$GITHUB_OUTPUT"

      - name: Configure AWS credentials (OIDC)
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789012:role/gha-ecr-push
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build & push image
        env:
          REGISTRY: ${{ steps.ecr.outputs.registry }}
          TAG: ${{ steps.meta.outputs.sha }}
        run: |
          docker build -t "$REGISTRY/$ECR_REPOSITORY:$TAG" .
          docker push     "$REGISTRY/$ECR_REPOSITORY:$TAG"

  bump-config:
    needs: build-test-push
    runs-on: ubuntu-latest
    steps:
      - name: Checkout config repo
        uses: actions/checkout@v4
        with:
          repository: acme/banking-config
          token: ${{ secrets.CONFIG_REPO_PAT }}

      - name: Install kustomize
        run: |
          curl -sSL https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh | bash
          sudo mv kustomize /usr/local/bin/

      - name: Bump image tag in staging overlay
        env:
          TAG: ${{ needs.build-test-push.outputs.image_tag }}
        run: |
          cd overlays/staging
          kustomize edit set image \
            backend=123456789012.dkr.ecr.eu-west-1.amazonaws.com/banking/backend:${TAG}

      - name: Commit the bump
        env:
          TAG: ${{ needs.build-test-push.outputs.image_tag }}
        run: |
          git config user.name  "ci-bot"
          git config user.email "ci-bot@users.noreply.github.com"
          git commit -am "deploy(staging): backend ${TAG}"
          git push
          # For prod: open a PR instead of pushing, e.g. with `gh pr create`,
          # so a human approves promotion before Argo CD syncs prod.
```

---

## 4. Kubernetes Manifests for the Backend

**What.** The stateless backend declared as a `Deployment`, fronted by a
`Service`, scaled by a `HorizontalPodAutoscaler`, and exposed through an
`Ingress` backed by the AWS Load Balancer Controller (ALB).

**Why these choices.**
- **Resource requests/limits** give the scheduler accurate bin-packing
  information and protect noisy-neighbour isolation; requests also drive HPA and
  cluster-autoscaler decisions.
- **Liveness vs readiness probes** are split: liveness restarts a wedged
  process; readiness gates traffic until the app (and its DB pool) is warm.
  Spring Boot Actuator exposes both groups natively.
- **`securityContext` non-root + read-only root FS** matches the Dockerfile and
  satisfies banking hardening baselines.
- **RollingUpdate with `maxUnavailable: 0`** guarantees no capacity dip during
  deploys — new pods must be Ready before old ones drain.

**How.** Probes hit `/actuator/health/liveness` and `/readiness`; env comes from
the k8s Secret that External Secrets Operator populates (§8); the ALB Ingress
terminates TLS via ACM.

```yaml
# base/backend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  labels: { app: backend }
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  selector:
    matchLabels: { app: backend }
  template:
    metadata:
      labels: { app: backend }
    spec:
      serviceAccountName: backend          # IRSA-bound (see §9)
      securityContext:
        runAsNonRoot: true
        runAsUser: 10001
        fsGroup: 10001
        seccompProfile: { type: RuntimeDefault }
      containers:
        - name: backend
          image: backend                    # tag set by kustomize/CI
          ports:
            - { name: http, containerPort: 8080 }
          envFrom:
            - secretRef: { name: backend-secrets }   # from ExternalSecret
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: prod
          resources:
            requests: { cpu: "500m", memory: "768Mi" }
            limits:   { cpu: "1500m", memory: "1536Mi" }
          livenessProbe:
            httpGet:  { path: /actuator/health/liveness, port: http }
            initialDelaySeconds: 40
            periodSeconds: 15
            failureThreshold: 3
          readinessProbe:
            httpGet:  { path: /actuator/health/readiness, port: http }
            initialDelaySeconds: 20
            periodSeconds: 10
            failureThreshold: 3
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities: { drop: ["ALL"] }
          volumeMounts:
            - { name: tmp, mountPath: /tmp }
      volumes:
        - name: tmp
          emptyDir: {}
```

```yaml
# base/backend-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: backend
spec:
  type: ClusterIP
  selector: { app: backend }
  ports:
    - { name: http, port: 80, targetPort: http }
```

```yaml
# base/backend-hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: backend
  minReplicas: 3
  maxReplicas: 12
  metrics:
    - type: Resource
      resource:
        name: cpu
        target: { type: Utilization, averageUtilization: 65 }
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
```

```yaml
# base/backend-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: backend
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS":443}]'
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:eu-west-1:123456789012:certificate/abcd-efgh
    alb.ingress.kubernetes.io/healthcheck-path: /actuator/health/readiness
    alb.ingress.kubernetes.io/ssl-redirect: '443'
spec:
  rules:
    - host: api.bank.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: backend
                port: { number: 80 }
```

---

## 5. The Outbox Relay Deployment

**What.** A *separate* Deployment running the same image but with a profile that
activates only the **transactional outbox relay** — the background worker that
reads unpublished rows from the `outbox` table and publishes them to Amazon MQ
(RabbitMQ).

**Why `replicas: 1`.** The relay must publish each event **exactly once** (at
least, never *double*-publish). If two replicas polled the same outbox rows
concurrently, a banking event (e.g. a transfer-completed message) could be
emitted twice, causing duplicate downstream processing. Running a single active
replica is the simplest correct design. The trade-off is availability: if the
single pod dies, publishing pauses until Kubernetes reschedules it (seconds).
Because the outbox is durable in PostgreSQL, no events are *lost* — only briefly
delayed. We therefore set `strategy: Recreate` so we never have two relays
overlapping during a rollout.

**Alternative — leader election.** For higher availability, run multiple
replicas that elect a single leader (Spring Integration `LockRegistryLeader`
backed by a JDBC/Redis lock, or a Kubernetes `Lease` via the
`leader-election` API). Only the leader polls and publishes; on leader loss a
standby takes over within the lease TTL. This adds complexity for faster
failover — appropriate once publish latency SLAs tighten.

```yaml
# base/outbox-relay-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: outbox-relay
  labels: { app: outbox-relay }
spec:
  replicas: 1                 # exactly one active publisher
  strategy:
    type: Recreate            # never overlap two relays during rollout
  selector:
    matchLabels: { app: outbox-relay }
  template:
    metadata:
      labels: { app: outbox-relay }
    spec:
      serviceAccountName: backend
      securityContext:
        runAsNonRoot: true
        runAsUser: 10001
      containers:
        - name: outbox-relay
          image: backend       # same image, different role
          envFrom:
            - secretRef: { name: backend-secrets }
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: prod,relay        # enables @Scheduled outbox poller only
          resources:
            requests: { cpu: "200m", memory: "512Mi" }
            limits:   { cpu: "500m", memory: "768Mi" }
          livenessProbe:
            httpGet:  { path: /actuator/health/liveness, port: 8080 }
            initialDelaySeconds: 40
            periodSeconds: 20
```

---

## 6. Kustomize Structure

**What.** A `base/` holding environment-agnostic manifests, plus `overlays/` per
environment (`dev`, `staging`, `prod`) that patch the base.

**Why Kustomize.** It is template-free and built into `kubectl`; overlays are
plain YAML patches, so diffs are readable in PRs and Argo CD renders them
natively. Per-environment differences — replica counts, image tags, resource
sizes, host names — live as small overrides instead of duplicated full manifests
(DRY). The trade-off vs Helm: no loops/conditionals, but for a fixed set of
environments that simplicity is a feature.

**How.** The base lists every resource and a common image name; each overlay
sets the concrete image tag (bumped by CI) and patches the deltas.

```
banking-config/
├── base/
│   ├── kustomization.yaml
│   ├── backend-deployment.yaml
│   ├── backend-service.yaml
│   ├── backend-hpa.yaml
│   ├── backend-ingress.yaml
│   ├── outbox-relay-deployment.yaml
│   ├── external-secret.yaml
│   └── service-account.yaml
└── overlays/
    ├── dev/      { kustomization.yaml, patches… }
    ├── staging/  { kustomization.yaml, patches… }
    └── prod/     { kustomization.yaml, patches… }
```

```yaml
# base/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - service-account.yaml
  - external-secret.yaml
  - backend-deployment.yaml
  - backend-service.yaml
  - backend-hpa.yaml
  - backend-ingress.yaml
  - outbox-relay-deployment.yaml
images:
  - name: backend
    newName: 123456789012.dkr.ecr.eu-west-1.amazonaws.com/banking/backend
    newTag: PLACEHOLDER     # overridden per overlay by `kustomize edit set image`
commonLabels:
  app.kubernetes.io/part-of: banking-platform
```

```yaml
# overlays/prod/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: banking-prod
resources:
  - ../../base
images:
  - name: backend
    newName: 123456789012.dkr.ecr.eu-west-1.amazonaws.com/banking/backend
    newTag: "9f2c1a7b3e0d"     # immutable git SHA, bumped via reviewed PR
patches:
  - path: replicas-and-resources.yaml
    target: { kind: Deployment, name: backend }
```

```yaml
# overlays/prod/replicas-and-resources.yaml  (strategic-merge patch)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
spec:
  replicas: 6
  template:
    spec:
      containers:
        - name: backend
          resources:
            requests: { cpu: "1",   memory: "1536Mi" }
            limits:   { cpu: "2",   memory: "2560Mi" }
```

Overlays for `dev`/`staging` use the same shape with smaller numbers (e.g. dev:
`replicas: 1`, `cpu: 250m`).

---

## 7. Argo CD Setup

**What.** Argo CD runs inside EKS and continuously reconciles each environment's
overlay path in the config repo onto its namespace. We use the **app-of-apps**
pattern: one root Application points at a directory of child Applications, one
per environment.

**Why app-of-apps + automated sync.** A single root app bootstraps the entire
platform and keeps the *set* of environments itself under GitOps. Automated sync
with `selfHeal` reverts manual drift; `prune` deletes objects removed from Git so
the cluster never accumulates orphans. Prod typically keeps automated sync but
gates *what reaches its branch/path* behind PR review (promotion is a Git
action, §12).

**How — install (note).** Install via the official manifests or Helm:

```bash
kubectl create namespace argocd
kubectl apply -n argocd \
  -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

**Root app (app-of-apps):**

```yaml
# argocd/root-app.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: banking-root
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/acme/banking-config.git
    targetRevision: main
    path: argocd/apps           # directory of child Application manifests
  destination:
    server: https://kubernetes.default.svc
    namespace: argocd
  syncPolicy:
    automated: { prune: true, selfHeal: true }
```

**Per-environment child Application (prod shown):**

```yaml
# argocd/apps/prod.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: banking-prod
  namespace: argocd
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: default
  source:
    repoURL: https://github.com/acme/banking-config.git
    targetRevision: main
    path: overlays/prod
  destination:
    server: https://kubernetes.default.svc
    namespace: banking-prod
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
      - ApplyOutOfSyncOnly=true
    retry:
      limit: 5
      backoff: { duration: 10s, factor: 2, maxDuration: 3m }
```

`overlays/dev` and `overlays/staging` get analogous Applications pointing at
their paths and namespaces.

---

## 8. Secrets Management

**What.** **External Secrets Operator (ESO)** pulls secret material from **AWS
Secrets Manager** at runtime and projects it into native Kubernetes `Secret`
objects. A `SecretStore` defines *where* secrets come from; an `ExternalSecret`
defines *which* keys to fetch and *how* to shape the resulting `Secret`.

**Why never commit raw secrets.** Anything in Git is permanent and visible to
every repo reader and CI job — unacceptable for DB credentials or JWT signing
keys. ESO keeps the **source of truth in Secrets Manager** (encrypted, IAM-gated,
rotatable) while Git holds only a *reference*. The alternative, **Sealed
Secrets**, encrypts the secret so the *ciphertext* can be committed safely; we
prefer ESO here because Secrets Manager already holds our DB/MQ credentials and
supports automatic rotation.

**How.** ESO authenticates to AWS via IRSA (the operator's ServiceAccount is
bound to an IAM role allowing `secretsmanager:GetSecretValue`), reads the named
secrets every `refreshInterval`, and writes a `Secret` the backend consumes via
`envFrom`.

```yaml
# base/secret-store.yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets
spec:
  provider:
    aws:
      service: SecretsManager
      region: eu-west-1
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets      # IRSA-bound SA
```

```yaml
# base/external-secret.yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: backend-secrets
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets
    kind: SecretStore
  target:
    name: backend-secrets          # the k8s Secret that envFrom reads
    creationPolicy: Owner
  data:
    - secretKey: SPRING_DATASOURCE_URL
      remoteRef: { key: banking/prod/db, property: jdbc_url }
    - secretKey: SPRING_DATASOURCE_USERNAME
      remoteRef: { key: banking/prod/db, property: username }
    - secretKey: SPRING_DATASOURCE_PASSWORD
      remoteRef: { key: banking/prod/db, property: password }
    - secretKey: SPRING_RABBITMQ_ADDRESSES
      remoteRef: { key: banking/prod/mq, property: amqps_uri }
    - secretKey: JWT_SIGNING_KEY
      remoteRef: { key: banking/prod/jwt, property: private_key }
```

The only thing in Git is the *reference* (`banking/prod/db`), never the value.

---

## 9. Connecting to Managed AWS Services

**What.** Pods reach **RDS PostgreSQL**, **Amazon MQ (RabbitMQ)**, **S3**, and
**Secrets Manager** — all managed, all outside the cluster.

**Why this split.** Stateful services demand backup, PITR, Multi-AZ failover and
patching that AWS operates far more reliably than we could inside Kubernetes.
The database in particular is **never** run in-cluster (see infra doc). Pods stay
stateless and disposable.

**How.**
- **RDS / Amazon MQ:** reached over the network using connection strings injected
  as env vars from the ESO-managed Secret (§8). EKS worker nodes and the RDS/MQ
  instances live in **private subnets**; **security groups** allow only the node
  security group to reach the DB port (5432) and the broker port (5671/AMQPS).
- **S3:** accessed with **IRSA** (IAM Roles for Service Accounts) — no static
  keys. The pod's ServiceAccount is annotated with an IAM role granting scoped
  S3 access; the AWS SDK picks up temporary credentials automatically.

```yaml
# base/service-account.yaml — IRSA binding for the backend pods
apiVersion: v1
kind: ServiceAccount
metadata:
  name: backend
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/banking-backend-irsa
```

A short Terraform sketch for the EKS cluster, the IRSA role, and Multi-AZ RDS
(full networking lives in the infra doc):

```hcl
module "eks" {
  source          = "terraform-aws-modules/eks/aws"
  cluster_name    = "banking-prod"
  cluster_version = "1.30"
  vpc_id          = var.vpc_id
  subnet_ids      = var.private_subnet_ids   # nodes in private subnets

  eks_managed_node_groups = {
    default = {
      instance_types = ["m6i.large"]
      min_size       = 3
      max_size       = 12
      desired_size   = 4
    }
  }
  enable_irsa = true
}

# IRSA role granting the backend ServiceAccount scoped S3 access
module "backend_irsa" {
  source                = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  role_name             = "banking-backend-irsa"
  oidc_providers = {
    main = {
      provider_arn               = module.eks.oidc_provider_arn
      namespace_service_accounts = ["banking-prod:backend"]
    }
  }
}

resource "aws_db_instance" "postgres" {
  identifier              = "banking-prod"
  engine                  = "postgres"
  engine_version          = "16"
  instance_class          = "db.m6g.large"
  allocated_storage       = 100
  multi_az                = true             # synchronous standby
  backup_retention_period = 14               # PITR window
  storage_encrypted       = true
  db_subnet_group_name    = aws_db_subnet_group.private.name
  vpc_security_group_ids  = [aws_security_group.rds.id]
  deletion_protection     = true
}
```

---

## 10. Observability In-Cluster

**What.** **Prometheus** scrapes metrics, **Grafana** visualizes them, and
**Alertmanager** routes alerts — typically installed via the
`kube-prometheus-stack` Helm chart. A `ServiceMonitor` tells Prometheus to scrape
the backend's Actuator `/actuator/prometheus` endpoint.

**Why the Operator/ServiceMonitor approach.** It is itself declarative and
GitOps-friendly: a `ServiceMonitor` is a CRD committed to Git, so monitoring
config follows the same review-and-reconcile flow as everything else, instead of
hand-edited Prometheus config files.

**How — install note.**

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install kube-prom prometheus-community/kube-prometheus-stack -n monitoring --create-namespace
```

```yaml
# base/servicemonitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: backend
  labels: { release: kube-prom }   # matches the Prometheus selector
spec:
  selector:
    matchLabels: { app: backend }
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
```

```yaml
# base/prometheus-rules.yaml — example alert rule
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: backend-alerts
  labels: { release: kube-prom }
spec:
  groups:
    - name: backend.rules
      rules:
        - alert: BackendHighErrorRate
          expr: |
            sum(rate(http_server_requests_seconds_count{status=~"5..",app="backend"}[5m]))
              /
            sum(rate(http_server_requests_seconds_count{app="backend"}[5m])) > 0.02
          for: 5m
          labels: { severity: critical }
          annotations:
            summary: "Backend 5xx error rate >2% for 5m"
        - alert: OutboxRelayDown
          expr: up{app="outbox-relay"} == 0
          for: 2m
          labels: { severity: critical }
          annotations:
            summary: "Outbox relay is not being scraped — publishing may be stalled"
```

---

## 11. Rollout, Rollback & Disaster Recovery

**Rolling update behavior.** With `maxUnavailable: 0` / `maxSurge: 1`, a deploy
brings up a new pod, waits for its readiness probe to pass, then terminates an
old one — repeating until all replicas run the new SHA. Capacity never dips, and
a pod that fails readiness halts the rollout automatically.

**Rollback = `git revert`.** Because the live state equals Git, rolling back is a
Git operation, not a cluster operation:

```bash
# Identify the bad deploy commit in the config repo
git log --oneline overlays/prod
# Revert it — restores the previous image tag / manifest
git revert <bad-sha>
git push
# Argo CD detects the change and syncs prod back to the known-good state.
```

This is auditable (the revert is itself a reviewed commit) and uniform with how
every other change ships. Argo CD also retains revision history, so
`argocd app rollback banking-prod <revision>` is available for emergencies, but
the Git-first path is preferred so Git never drifts from the cluster.

**Canary / blue-green (optional).** For higher-risk releases, **Argo Rollouts**
replaces the Deployment with a `Rollout` resource supporting canary steps
(shift 10% → analyze metrics → 50% → 100%) or blue-green (full parallel stack,
instant switch, instant abort). This is optional; the default RollingUpdate is
sufficient for most backend changes.

**Disaster recovery.** Cluster and app state are reproducible from Git, and ECR
holds every image by SHA — so the *compute* tier is disposable. **Data
durability and DR live in the infrastructure document:** RDS runs **Multi-AZ**
with a synchronous standby and **point-in-time recovery** from continuous
backups, targeting **RPO ≤ 5 min** and **RTO ≤ 1 hr**. The database is never in
Kubernetes, so a full cluster loss costs no data — rebuild the cluster from Git
and reconnect to the surviving RDS instance.

---

## 12. Promotion Flow & Summary

**Promotion flow (dev → staging → prod).** Promotion is just moving a tested
image SHA up the overlay ladder via reviewed PRs:

```
build image:<sha>  ──▶  overlays/dev      (auto-bumped by CI, Argo syncs dev)
        │
        ├─ soak / smoke tests pass
        ▼
PR: bump overlays/staging to :<sha>  ──▶  review/merge  ──▶  Argo syncs staging
        │
        ├─ QA / UAT sign-off
        ▼
PR: bump overlays/prod to :<sha>     ──▶  approval/merge ──▶  Argo syncs prod
```

The *same image artifact* is promoted unchanged — only the tag reference moves
between overlays — so what you tested is exactly what ships. Each promotion is a
small, reviewable Git diff, and rollback is the revert of that diff.

**Summary — component → tool.**

| Component / Concern            | Tool / Mechanism                                  |
|--------------------------------|---------------------------------------------------|
| Container runtime / orchestr.  | AWS EKS (managed Kubernetes)                       |
| Backend packaging              | Multi-stage Dockerfile → single image (slim JRE)  |
| Image registry                 | Amazon ECR, tagged by immutable git SHA           |
| CI (build/test/push/bump)      | GitHub Actions + OIDC to AWS                       |
| Deployment config / templating | Kustomize (base + per-env overlays)               |
| Continuous delivery / sync     | Argo CD (app-of-apps, automated sync, self-heal)  |
| Rollback                       | `git revert` in config repo → Argo CD reconciles  |
| Backend workload               | Deployment (N stateless replicas) + Service + HPA |
| Event publishing               | Outbox relay Deployment (replicas:1 / leader-elect)|
| External traffic / TLS         | Ingress → AWS Load Balancer Controller (ALB) + ACM|
| Autoscaling                    | HorizontalPodAutoscaler (CPU) + cluster autoscaler|
| Secrets                        | External Secrets Operator ← AWS Secrets Manager   |
| Cloud IAM for pods             | IRSA (IAM Roles for Service Accounts)             |
| Relational data                | Amazon RDS PostgreSQL (Multi-AZ, PITR) — outside k8s|
| Messaging                      | Amazon MQ (RabbitMQ) — outside k8s                |
| Object storage                 | Amazon S3 (accessed via IRSA)                     |
| Metrics / dashboards / alerts  | Prometheus + Grafana + Alertmanager (ServiceMonitor)|
| Dashboard (React)              | S3 + CloudFront static hosting (not in k8s)       |
| Mobile (React Native)          | App stores (not deployed via k8s)                 |

GitOps gives this platform a single auditable source of truth, automated drift
correction, and one-commit rollback — exactly the controls a regulated banking
system needs, while keeping all durable state in managed AWS services outside the
cluster.

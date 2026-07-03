# Backend API — Features & Implementation

> Digital Banking Platform · Backend Feature Documentation
> Status: Baseline · Owner: Backend Lead
> Runtime: Java 17 · Spring Boot 3 · PostgreSQL · RabbitMQ

---

## Overview

The backend is a single deployable **modular monolith** built on Java 17 and
Spring Boot 3. It is internally divided into clean modules — `auth`,
`accounts`, `wallets`, `ledger`, `transfers`, `kyc`, `audit`, and `events` —
that talk to each other through well-defined service interfaces. There is **one
PostgreSQL** database that is the system-of-record for money.

Two rules drive every design decision in this document:

1. **Money moves synchronously, inside one ACID transaction.** A money
   operation either commits completely or not at all. Events never move money.
2. **Side-effects are event-driven via the transactional outbox.** Notifications,
   projections, and downstream fan-out are published *after* the money
   transaction commits, derived from rows written in that same transaction.

Each feature below is documented as: **(a) what** it is, **(b) why** it is
designed this way, **(c) how** it works, and **(d) real code**.

---

## 1. Project Structure & Module Layout

**What.** A **package-by-feature** layout inside one Gradle/Maven project. Each
feature is a package holding its own web, domain, and persistence layers
(controller + DTOs, service interface + implementation, entity + repository).
This is the opposite of **package-by-layer** (one big `controller/`,
`service/`, `repository/`), which scatters a single feature across the codebase.

**Why package-by-feature.** A change to transfers stays in the `transfers`
package instead of touching four layer-packages; related code sits together;
and a future extraction of a module into its own service is a matter of moving
one package rather than untangling shared layers.

**Layered inside each feature.** A feature package is **not** flat — it is split
into three explicit layers, each a sub-package with one responsibility:

| Layer         | Holds                                  | Depends on            |
|---------------|----------------------------------------|-----------------------|
| `web/`        | `*Controller`, request/response DTOs   | `domain/` (interface) |
| `domain/`     | service **interface** + business logic | nothing outward       |
| `persistence/`| `*Repository`, JPA `*Entity`           | implements ports the domain owns |

The dependency arrow always points **inward** toward `domain/`: `web/` and
`persistence/` know about the domain, never the reverse. This is the structural
expression of the Dependency Inversion Principle — the core logic depends on
abstractions, and the delivery (web) and infrastructure (DB) layers plug into it.

**Interface + implementation by default.** Every service is declared as an
**interface** in `domain/` with a `Default*` implementation beside it —
`OrderService` + `DefaultOrderService`, `TransferService` +
`DefaultTransferService`, and so on:

| Why interface-first                                | Pay-off                                       |
|----------------------------------------------------|-----------------------------------------------|
| Callers depend on the **port**, not the class      | Dependency Inversion is uniform, not selective |
| Tests mock the interface, no Spring context needed | Fast, isolated unit tests for every service    |
| A second implementation never forces a refactor    | Open/Closed: add an impl, callers untouched    |
| One predictable shape across all features          | Lower cognitive load — every feature reads the same |

The `Default*` prefix marks the production implementation; a test stub or an
alternate strategy is just another class against the same interface.

**How — the shape of one feature package.** Each module is layered into
`web/`, `domain/`, and `persistence/`:

```text
transfers/
├── web/
│   ├── TransferController.java      # @RestController  /v1/transfers
│   └── dto/
│       ├── TransferRequest.java     # request record (validated)
│       └── TransferResult.java      # response record
├── domain/
│   ├── TransferService.java         # INTERFACE — the port callers depend on
│   └── DefaultTransferService.java  # @Service @Transactional debit+credit+outbox
└── persistence/
    ├── Transfer.java                # @Entity
    ├── Transaction.java             # @Entity
    ├── TransferRepository.java      # extends JpaRepository
    └── TransactionRepository.java
```

### Full package tree

```text
com.bank
├── BankApplication.java                 # @SpringBootApplication entry point
│
├── shared/                              # DRY: cross-cutting, no business logic
│   ├── exception/
│   │   ├── ApiError.java                # {code, message, traceId} envelope
│   │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   │   └── DomainException.java         # base for typed business errors
│   ├── entity/
│   │   └── BaseAuditEntity.java         # id, createdAt, updatedAt, version
│   └── utils/
│       ├── Money.java                   # BigDecimal helper, minor-unit safe
│       ├── CurrentUser.java             # authenticated principal accessor
│       └── PageResponse.java            # pagination wrapper
│
├── config/                             # SRP: app configuration only
│   ├── SecurityConfig.java
│   ├── JwtAuthFilter.java              # OncePerRequestFilter
│   └── RabbitTopologyConfig.java       # exchanges, queues, bindings
│
└── feature/
    ├── auth/
    │   ├── web/
    │   │   ├── AuthController.java      # POST /v1/auth/{register,login,refresh,logout}
    │   │   └── dto/  (LoginRequest, RegisterRequest, TokenPair)
    │   ├── domain/
    │   │   ├── AuthService.java         # INTERFACE
    │   │   ├── DefaultAuthService.java  # @Service
    │   │   ├── JwtService.java          # INTERFACE — RS256 sign/verify
    │   │   └── DefaultJwtService.java   # @Service
    │   └── persistence/
    │       ├── User.java  RefreshToken.java          # @Entity (refresh: hashed, sessionId family)
    │       └── UserRepository.java  RefreshTokenRepository.java
    │
    ├── rbac/
    │   ├── domain/
    │   │   ├── PermissionService.java          # INTERFACE — resolve effective permissions
    │   │   ├── DefaultPermissionService.java   # @Service
    │   │   └── PermissionEvaluator.java        # backs @PreAuthorize("hasPermission(...)")
    │   └── persistence/
    │       ├── Role.java  Permission.java  UserRole.java     # @Entity
    │       └── RoleRepository.java  PermissionRepository.java
    │
    ├── accounts/
    │   ├── web/        AccountController.java   # /v1/accounts  + dto/
    │   ├── domain/     AccountService.java + DefaultAccountService.java   # @Service
    │   └── persistence/ Account.java  AccountStatus.java  AccountRepository.java
    │
    ├── wallets/
    │   ├── web/        WalletController.java    # /v1/wallets  + dto/
    │   ├── domain/     WalletService.java + DefaultWalletService.java     # balance derived from ledger
    │   └── persistence/ Wallet.java  WalletStatus.java  WalletRepository.java
    │
    ├── ledger/                              # append-only money truth
    │   ├── domain/
    │   │   ├── LedgerService.java           # INTERFACE — post(), balanceOf()
    │   │   └── DefaultLedgerService.java    # @Service
    │   └── persistence/
    │       ├── LedgerEntry.java             # @Entity, immutable
    │       ├── Direction.java               # DEBIT | CREDIT
    │       └── LedgerEntryRepository.java
    │
    ├── transfers/                          # the money core
    │   ├── web/        TransferController.java  # /v1/transfers, /v1/transactions + dto/
    │   ├── domain/     TransferService.java + DefaultTransferService.java  # @Transactional debit+credit+outbox
    │   └── persistence/ Transfer.java  Transaction.java  TransferRepository.java  TransactionRepository.java
    │
    ├── kyc/
    │   ├── web/        KycController.java       # /v1/kyc  + dto/
    │   ├── domain/
    │   │   ├── KycService.java + DefaultKycService.java     # case state machine
    │   │   ├── KycVendorClient.java            # INTERFACE — vendor port
    │   │   └── HttpKycVendorClient.java        # real impl (test stub is the alternate)
    │   └── persistence/ KycCase.java  KycStatus.java  KycCaseRepository.java
    │
    ├── audit/
    │   ├── web/        AuditController.java     # /v1/audit  + dto/
    │   ├── domain/
    │   │   ├── AuditService.java + DefaultAuditService.java  # append(), query()
    │   │   └── AuditAspect.java                # @Audited AOP
    │   └── persistence/ AuditRecord.java  AuditRecordRepository.java     # @Entity, append-only
    │
    └── events/                             # the event backbone
        ├── domain/
        │   ├── DomainEvent.java
        │   ├── EventPublisher.java         # INTERFACE — outbox port
        │   ├── OutboxEventPublisher.java   # real impl (in-memory is the test alternate)
        │   └── OutboxRelay.java            # @Scheduled publisher → RabbitMQ
        ├── persistence/ OutboxEvent.java  OutboxEventRepository.java
        └── web/        NotificationConsumer.java   # @RabbitListener, idempotent
```

> Every service is an **interface + `Default*` implementation** in its feature's
> `domain/` layer. `KycVendorClient` and `EventPublisher` additionally have a
> *second* production-relevant implementation (test stub / in-memory), but the
> interface-first rule is uniform — callers always depend on the port, never the
> class.

### Resources & build layout

```text
src/main/resources
├── application.yml                  # base config
├── application-dev.yml              # profile overrides
├── application-prod.yml
├── db/migration/                    # Flyway, ordered, immutable once shipped
│   ├── V1__core.sql                 # users, accounts, wallets, rbac
│   ├── V2__ledger.sql               # ledger_entries, transactions, transfers
│   ├── V3__kyc.sql
│   ├── V4__audit.sql
│   └── V5__outbox.sql
└── logback-spring.xml               # structured JSON logs with traceId

src/test/java/com/bank/feature
├── transfers/   DefaultTransferServiceTest.java   # double-entry + idempotency (mocks the port)
├── auth/        RefreshRotationTest.java
└── support/     PostgresTestContainer.java, TestDataFactory.java
```

Tests live in the same package as the feature they exercise, mirroring `src/main`.
Because each service is an interface, unit tests target the `Default*` impl and
mock collaborators through their ports — no Spring context needed.

### Conventions

- Every feature is split into `web/` (controllers + DTOs), `domain/` (service
  **interface** + `Default*` impl + business logic), and `persistence/`
  (repositories + JPA entities). Dependencies point inward toward `domain/`.
- Every service is an **interface + `Default*` implementation**; callers depend
  on the interface, never the concrete class.
- DTOs are Java `record`s under `web/dto/`; entities stay in `persistence/`.
- All money-moving logic is confined to `ledger/` + `transfers/`; no other
  feature writes `ledger_entries`.
- `shared/` holds only cross-cutting plumbing — `exception/` (error envelope +
  handler), `entity/` (`BaseAuditEntity`), `utils/` (`Money`, paging) — and
  `config/` holds app-wide configuration only. Neither holds business logic.

### How the structure follows SOLID / DRY / KISS / YAGNI

The feature-based, layered structure isn't arbitrary — each principle maps to a
concrete structural decision:

| Principle | In this structure |
|-----------|-------------------|
| **S** — Single Responsibility | Each layer has one job: `web/` (HTTP edge + DTOs), `domain/` (business logic behind a service interface), `persistence/` (repositories + entities). `LedgerService` is the **only** writer of `ledger_entries` — money correctness has one owner. |
| **O** — Open/Closed | New side-effects are added as **new event consumers** in `events/`; the producer (`DefaultTransferService`) never changes. A new behaviour is a new implementation of an existing service interface, not an edit to a caller. |
| **L** — Liskov Substitution | Every `Default*` impl is fully substitutable for its interface, and alternates (`HttpKycVendorClient` vs. a test stub, outbox vs. in-memory publisher) honor the same contract the caller relies on. |
| **I** — Interface Segregation | Service interfaces are per-feature and narrow; DTOs are small per-use-case `record`s (`TransferRequest`, `TransferResult`), not one fat shared object. Callers depend only on the operations they need. |
| **D** — Dependency Inversion | High-level logic depends on **abstractions it owns**: `web/` → `domain/` service *interface*, and `domain/` → repository/port — never on a concrete class or RabbitMQ directly. The `web/ → domain/ ← persistence/` arrows all point inward. All wiring is **constructor injection**. |
| **DRY** | One `shared/` source of truth per cross-cutting decision: `exception/` (one error shape via `ApiError` + `GlobalExceptionHandler`), `entity/BaseAuditEntity` (one audit-column set), `utils/Money` (one money type). |
| **KISS** | A uniform three-layer shape (`web`/`domain`/`persistence`) every feature follows, so any module reads the same way; **derived** balance (`SUM(ledger_entries)`) instead of a cached column + invalidation; a single `@Transactional` money path instead of a saga. |
| **YAGNI** | The structure stops at three layers and one interface per service — no `api/internal/impl` micro-layers, no microservices, no generic base-service, no premature caching until a measured need appears. |

> The through-line: give each class and each layer one job behind an explicit
> interface (SOLID), inject collaborators through the ports they own (DIP), and
> keep one source of truth for each cross-cutting concern in `shared/` (DRY) —
> with a single, predictable feature shape rather than ad-hoc structure.

---

## 2. Authentication

**What.** Registration, password login, stateless **JWT access tokens** signed
with **RS256** (short TTL), and **rotating refresh tokens** that are stored
hashed. Re-use of an already-rotated refresh token is treated as theft and
revokes the entire session family.

**Why.**

- **Stateless JWT access tokens** mean the API does not hit the database to
  validate every request — the signature and claims are enough. Short TTL
  (e.g. 10 minutes) limits the blast radius of a leaked token.
- **RS256 (asymmetric)** lets the auth module hold the private key while every
  other module (or a future separate service) verifies with the public key. No
  shared secret to leak.
- **Rotating refresh tokens** give long sessions without long-lived bearer
  credentials. Each refresh issues a new refresh token and invalidates the old.
- **Reuse detection** is the key security property: if an old (already rotated)
  token is presented, either the client is buggy or an attacker replayed a
  stolen token. We cannot tell which, so we revoke the whole session family —
  fail safe.

**How.** Refresh tokens are persisted as a linked chain (`previousId`) within a
`sessionId` family. We store only a SHA-256 hash of the secret. On refresh we
look up by hash; if the matched token is already `rotated` or `revoked`, we
revoke every token sharing that `sessionId`.

### SecurityConfig

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)         // stateless API, no cookies
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v1/auth/register", "/v1/auth/login",
                                 "/v1/auth/refresh").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated())             // default-deny everything else
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) ->
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

### JwtService (RS256 issue + verify)

```java
@Service
public class DefaultJwtService implements JwtService {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final Duration accessTtl;

    public JwtService(KeyProvider keys,
                          @Value("${security.jwt.access-ttl:PT10M}") Duration accessTtl) {
        this.privateKey = keys.privateKey();
        this.publicKey = keys.publicKey();
        this.accessTtl = accessTtl;
    }

    public String issueAccessToken(User user, Set<String> permissions) {
        Instant now = Instant.now();
        return JWT.create()
            .withSubject(user.getId().toString())
            .withIssuer("bank-core")
            .withClaim("email", user.getEmail())
            .withArrayClaim("perms", permissions.toArray(String[]::new))
            .withIssuedAt(now)
            .withExpiresAt(now.plus(accessTtl))
            .withJWTId(UUID.randomUUID().toString())
            .sign(Algorithm.RSA256(publicKey, privateKey));
    }

    public DecodedJWT verify(String token) {
        return JWT.require(Algorithm.RSA256(publicKey, null))
            .withIssuer("bank-core")
            .build()
            .verify(token);   // throws JWTVerificationException on tamper/expiry
    }
}
```

### JwtAuthFilter

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                DecodedJWT jwt = jwtService.verify(header.substring(7));
                List<SimpleGrantedAuthority> authorities = jwt.getClaim("perms")
                    .asList(String.class).stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
                var auth = new UsernamePasswordAuthenticationToken(
                    jwt.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JWTVerificationException ex) {
                SecurityContextHolder.clearContext();   // invalid → stays anonymous
            }
        }
        chain.doFilter(req, res);
    }
}
```

### RefreshToken entity + repository

```java
@Entity
@Table(name = "refresh_tokens",
       indexes = @Index(name = "ix_refresh_hash", columnList = "tokenHash", unique = true))
public class RefreshToken {

    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID sessionId;            // family id; reuse revokes the whole family

    @Column(nullable = false, length = 64)
    private String tokenHash;          // SHA-256 hex of the secret, never the secret

    private UUID previousId;           // chain link to the token this rotated from

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenStatus status;        // ACTIVE, ROTATED, REVOKED

    @Column(nullable = false)
    private Instant expiresAt;

    // getters / setters / protected no-arg ctor omitted
}
```

```java
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken t set t.status = 'REVOKED' where t.sessionId = :sessionId")
    void revokeSession(@Param("sessionId") UUID sessionId);
}
```

### AuthController

```java
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public TokenPair login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.email(), req.password());
    }

    @PostMapping("/refresh")
    public TokenPair refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }
}
```

### AuthService (rotation + reuse detection)

```java
@Service
public class DefaultAuthService implements AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PermissionService rbac;
    private final JwtService jwtService;
    private final PasswordEncoder encoder;
    private final Duration refreshTtl;

    // constructor injection omitted for brevity

    @Override
    @Transactional
    public TokenPair login(String email, String rawPassword) {
        User user = users.findByEmail(email)
            .filter(u -> encoder.matches(rawPassword, u.getPasswordHash()))
            .orElseThrow(() -> new ApiException("AUTH_INVALID", "Invalid credentials", 401));
        return issuePair(user, UUID.randomUUID());     // new session family
    }

    @Override
    @Transactional
    public TokenPair refresh(String presentedSecret) {
        String hash = Hashing.sha256Hex(presentedSecret);
        RefreshToken token = refreshTokens.findByTokenHash(hash)
            .orElseThrow(() -> new ApiException("AUTH_REFRESH_UNKNOWN", "Unknown token", 401));

        // REUSE DETECTION: an already-rotated/revoked token means replay → kill the family
        if (token.getStatus() != TokenStatus.ACTIVE || token.getExpiresAt().isBefore(Instant.now())) {
            refreshTokens.revokeSession(token.getSessionId());
            throw new ApiException("AUTH_REFRESH_REUSE", "Refresh token reuse detected", 401);
        }

        token.setStatus(TokenStatus.ROTATED);          // single-use
        User user = users.findById(token.getUserId()).orElseThrow();
        return issuePair(user, token.getSessionId(), token.getId());
    }

    private TokenPair issuePair(User user, UUID sessionId) {
        return issuePair(user, sessionId, null);
    }

    private TokenPair issuePair(User user, UUID sessionId, UUID previousId) {
        Set<String> perms = rbac.permissionsFor(user.getId());
        String access = jwtService.issueAccessToken(user, perms);

        String secret = SecureRandoms.urlToken(48);    // opaque high-entropy string
        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setSessionId(sessionId);
        rt.setPreviousId(previousId);
        rt.setTokenHash(Hashing.sha256Hex(secret));
        rt.setStatus(TokenStatus.ACTIVE);
        rt.setExpiresAt(Instant.now().plus(refreshTtl));
        refreshTokens.save(rt);

        return new TokenPair(access, secret);
    }
}
```

---

## 3. Authorization / RBAC

**What.** Role-based access control with fine-grained permissions. Roles are
`{customer, support, operator, compliance, admin, auditor}`. A user can hold
several roles; effective permissions are the union of all roles' permissions.
Enforcement is **default-deny**: nothing is allowed unless a permission grants
it. Privileged actions are audited.

**Why.** Roles are convenient for assignment but coarse for enforcement.
Binding endpoints to *permissions* (e.g. `transfer:create`,
`kyc:review`) rather than role names means we can re-shape roles without
touching controllers. Default-deny is the only safe posture for a bank: a
missing rule must mean "no", never "yes".

**How.** Permissions are baked into the JWT `perms` claim at login (see §2), so
enforcement is a stateless claim check. We use Spring method security with
`@PreAuthorize("hasAuthority('...')")`. The `perms` are computed by joining
`user_roles → role_permissions → permissions`.

### Entities

```java
@Entity @Table(name = "roles")
public class Role {
    @Id @GeneratedValue private UUID id;
    @Column(unique = true, nullable = false) private String name;   // e.g. "compliance"

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();
    // accessors omitted
}

@Entity @Table(name = "permissions")
public class Permission {
    @Id @GeneratedValue private UUID id;
    @Column(unique = true, nullable = false) private String name;   // e.g. "transfer:create"
    // accessors omitted
}

@Entity @Table(name = "user_roles",
    uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "roleId"}))
public class UserRole {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private UUID roleId;
    // accessors omitted
}
```

### Permission resolution

```java
@Service
public class DefaultPermissionService implements PermissionService {

    private final EntityManager em;

    public DefaultPermissionService(EntityManager em) { this.em = em; }

    @Transactional(readOnly = true)
    public Set<String> permissionsFor(UUID userId) {
        return new HashSet<>(em.createQuery("""
                select distinct p.name
                from UserRole ur
                join Role r on r.id = ur.roleId
                join r.permissions p
                where ur.userId = :uid
                """, String.class)
            .setParameter("uid", userId)
            .getResultList());
    }
}
```

### Protected endpoint

```java
@RestController
@RequestMapping("/v1/admin/users")
public class UserAdminController {

    private final UserAdminService service;

    public UserAdminController(UserAdminService service) { this.service = service; }

    // default-deny: only principals whose JWT carries this authority pass
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:assign-role')")
    public void assignRole(@PathVariable UUID id, @RequestBody AssignRoleRequest req) {
        service.assignRole(id, req.role());   // service emits an AuditRecord
    }
}
```

---

## 4. Accounts & Wallets

**What.** An **Account** is the customer's banking relationship; a **Wallet** is
a currency-scoped balance container under an account. Both have explicit
lifecycle states. A wallet's **balance is never stored** — it is derived by
summing ledger entries.

**Why.** Storing a mutable `balance` column invites drift: a bug, a missed
update, or a partial failure leaves the number wrong with no way to detect it.
By deriving balance from an append-only ledger, the balance is always provably
the sum of immutable facts. Lifecycle states let us freeze a wallet (fraud
hold) without deleting anything.

**How.** `AccountStatus` and `WalletStatus` are explicit enums with guarded
transitions. The read path sums `ledger_entries`; for hot wallets a materialized
balance projection can be added later as a cache that is *rebuildable* from the
ledger, never the source of truth.

```java
@Entity @Table(name = "accounts")
public class Account {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false) private UUID ownerUserId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AccountStatus status;          // PENDING, ACTIVE, FROZEN, CLOSED

    @Column(nullable = false) private Instant createdAt;
    // accessors omitted
}

@Entity @Table(name = "wallets",
    uniqueConstraints = @UniqueConstraint(columnNames = {"accountId", "currency"}))
public class Wallet {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false) private UUID accountId;
    @Column(nullable = false, length = 3) private String currency;   // ISO-4217

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private WalletStatus status;           // ACTIVE, FROZEN, CLOSED
    // accessors omitted
}
```

### Balance derived from the ledger

```java
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("""
        select coalesce(sum(case when e.direction = 'CREDIT' then e.amount
                                 else -e.amount end), 0)
        from LedgerEntry e
        where e.walletId = :walletId
        """)
    BigDecimal deriveBalance(@Param("walletId") UUID walletId);
}
```

### REST endpoints

```java
@RestController
@RequestMapping("/v1/wallets")
public class WalletController {

    private final WalletService wallets;

    public WalletController(WalletService wallets) { this.wallets = wallets; }

    @PostMapping
    @PreAuthorize("hasAuthority('wallet:create')")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletView open(@Valid @RequestBody OpenWalletRequest req) {
        return wallets.open(req.accountId(), req.currency());
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("hasAuthority('wallet:read')")
    public BalanceView balance(@PathVariable UUID id) {
        return new BalanceView(id, wallets.balance(id));   // summed from ledger
    }
}
```

| Endpoint                         | Method | Permission       | Purpose                       |
|----------------------------------|--------|------------------|-------------------------------|
| `/v1/accounts`                   | POST   | `account:create` | Open an account (PENDING)     |
| `/v1/accounts/{id}/activate`     | POST   | `account:manage` | PENDING → ACTIVE (post-KYC)   |
| `/v1/wallets`                    | POST   | `wallet:create`  | Open a currency wallet        |
| `/v1/wallets/{id}/balance`       | GET    | `wallet:read`    | Derived balance               |
| `/v1/wallets/{id}/freeze`        | POST   | `wallet:manage`  | ACTIVE → FROZEN               |

---

## 5. Ledger, Transactions & Transfers

**What.** A **double-entry, append-only** ledger. Every money movement writes
balanced entries (total debits == total credits). A **transfer** is a paired
debit + credit committed in a **single `@Transactional`** unit. Money-moving
POSTs carry an **idempotency key** enforced by a unique DB constraint.
Insufficient funds are rejected. Reversals are done with **compensating
entries**, never by mutating or deleting history.

**Why.**

- **Double-entry** makes every transaction self-balancing and auditable; the
  whole system's net is always zero, which is a continuously checkable invariant.
- **Append-only** means history is immutable — a correction is itself a recorded
  event, which is what auditors and regulators require.
- **One ACID transaction** is the simplest correct way to move money: partial
  states (debit without credit) can never be observed.
- **Idempotency keys** make retries safe. Networks fail after the server
  committed; the client retries; the unique constraint guarantees the second
  attempt returns the original result instead of double-charging.
- **`BigDecimal` / `NUMERIC`** because binary floating point cannot represent
  decimal money exactly — `0.1 + 0.2 != 0.3` is unacceptable in a ledger.

**How.** `TransferService.transfer(...)` runs in one transaction: insert an
idempotency row (unique constraint), check funds via the derived balance, append
two `LedgerEntry` rows in a shared `transactionId`, and write an `OutboxEvent`
(see §8) — all committed together or all rolled back.

### Entities

```java
public enum Direction { DEBIT, CREDIT }

@Entity @Table(name = "ledger_entries")   // append-only: no update/delete
public class LedgerEntry {
    @Id @GeneratedValue private UUID id;

    @Column(nullable = false) private UUID transactionId;   // groups the paired legs
    @Column(nullable = false) private UUID walletId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Direction direction;

    @Column(nullable = false, precision = 19, scale = 4)    // NUMERIC(19,4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false) private Instant postedAt;
    @Column private String memo;
    // accessors; amount is always positive, sign comes from direction
}

@Entity @Table(name = "idempotency_keys",
    uniqueConstraints = @UniqueConstraint(name = "uq_idem_key", columnNames = "idemKey"))
public class IdempotencyRecord {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false) private String idemKey;
    @Column(nullable = false) private UUID transactionId;   // the result to replay
    @Column(nullable = false) private Instant createdAt;
    // accessors omitted
}
```

### TransferService

```java
@Service
public class DefaultTransferService implements TransferService {

    private final LedgerEntryRepository ledger;
    private final IdempotencyRepository idempotency;
    private final WalletRepository wallets;
    private final KycGate kycGate;
    private final OutboxWriter outbox;

    // constructor injection omitted

    @Transactional
    public TransferResult transfer(TransferCommand cmd) {

        // 1) Idempotency: insert-first. A duplicate key throws → we replay the prior result.
        try {
            idempotency.saveAndFlush(IdempotencyRecord.of(cmd.idempotencyKey(), cmd.transactionId()));
        } catch (DataIntegrityViolationException dup) {
            UUID existing = idempotency.findByIdemKey(cmd.idempotencyKey())
                .orElseThrow().getTransactionId();
            return TransferResult.replayed(existing);
        }

        Wallet from = wallets.getActive(cmd.fromWalletId());
        Wallet to   = wallets.getActive(cmd.toWalletId());

        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new ApiException("CURRENCY_MISMATCH", "Cross-currency not allowed", 422);
        }

        // 2) KYC gate: money-out requires a verified sender.
        kycGate.assertMoneyOutAllowed(from.getAccountId());

        // 3) Insufficient-funds check against the DERIVED balance.
        BigDecimal balance = ledger.deriveBalance(from.getId());
        if (balance.compareTo(cmd.amount()) < 0) {
            throw new ApiException("INSUFFICIENT_FUNDS",
                "Balance %s < amount %s".formatted(balance, cmd.amount()), 422);
        }

        // 4) Double-entry: two balanced legs sharing one transactionId.
        Instant now = Instant.now();
        ledger.save(leg(cmd.transactionId(), from.getId(), Direction.DEBIT,  cmd.amount(), from.getCurrency(), now, cmd.memo()));
        ledger.save(leg(cmd.transactionId(), to.getId(),   Direction.CREDIT, cmd.amount(), to.getCurrency(),   now, cmd.memo()));

        // 5) Outbox row in the SAME transaction — side-effects fan out after commit.
        outbox.write("transfer.completed", cmd.transactionId(),
            Map.of("from", from.getId(), "to", to.getId(),
                   "amount", cmd.amount(), "currency", from.getCurrency()));

        return TransferResult.posted(cmd.transactionId());
    }

    @Transactional
    public TransferResult reverse(UUID originalTxId, String reason) {
        List<LedgerEntry> legs = ledger.findByTransactionId(originalTxId);
        if (legs.isEmpty()) throw new ApiException("TX_NOT_FOUND", "Unknown transaction", 404);

        UUID reversalTxId = UUID.randomUUID();
        Instant now = Instant.now();
        // Compensating entries: each original leg gets an opposite-direction leg.
        for (LedgerEntry e : legs) {
            Direction opp = e.getDirection() == Direction.DEBIT ? Direction.CREDIT : Direction.DEBIT;
            ledger.save(leg(reversalTxId, e.getWalletId(), opp, e.getAmount(),
                            e.getCurrency(), now, "reversal:" + reason));
        }
        outbox.write("transfer.reversed", reversalTxId,
            Map.of("original", originalTxId, "reason", reason));
        return TransferResult.posted(reversalTxId);
    }

    private LedgerEntry leg(UUID txId, UUID walletId, Direction dir, BigDecimal amt,
                            String ccy, Instant at, String memo) {
        LedgerEntry e = new LedgerEntry();
        e.setTransactionId(txId);
        e.setWalletId(walletId);
        e.setDirection(dir);
        e.setAmount(amt);
        e.setCurrency(ccy);
        e.setPostedAt(at);
        e.setMemo(memo);
        return e;
    }
}
```

### Controller — idempotency key from header

```java
@RestController
@RequestMapping("/v1/transfers")
public class TransferController {

    private final TransferService transfers;

    public TransferController(TransferService transfers) { this.transfers = transfers; }

    @PostMapping
    @PreAuthorize("hasAuthority('transfer:create')")
    public ResponseEntity<TransferResult> create(
            @RequestHeader("Idempotency-Key") @NotBlank String idemKey,
            @Valid @RequestBody TransferRequest req) {

        var cmd = new TransferCommand(UUID.randomUUID(), req.fromWalletId(),
            req.toWalletId(), req.amount(), idemKey, req.memo());
        TransferResult result = transfers.transfer(cmd);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
            .body(result);
    }
}
```

The matching Flyway constraint that makes idempotency airtight:

```sql
CREATE TABLE idempotency_keys (
    id              UUID PRIMARY KEY,
    idem_key        TEXT NOT NULL,
    transaction_id  UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_idem_key UNIQUE (idem_key)   -- DB is the source of truth, not app memory
);

CREATE TABLE ledger_entries (
    id              UUID PRIMARY KEY,
    transaction_id  UUID NOT NULL,
    wallet_id       UUID NOT NULL,
    direction       TEXT NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
    amount          NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency        CHAR(3) NOT NULL,
    posted_at       TIMESTAMPTZ NOT NULL,
    memo            TEXT
);
CREATE INDEX ix_ledger_wallet ON ledger_entries (wallet_id);
CREATE INDEX ix_ledger_tx     ON ledger_entries (transaction_id);
-- No UPDATE/DELETE grants on ledger_entries: append-only at the privilege level.
```

---

## 6. KYC

**What.** Know-Your-Customer onboarding modeled as a **case state machine**.
Customers submit identity documents; an async vendor verifies them; the case
advances through states. Money-out is **gated** on a verified case.

**Why.** KYC is inherently asynchronous — a third-party vendor takes seconds to
minutes. We must not block a money transaction on a vendor call, and we must not
let unverified customers move funds. A state machine makes the allowed
transitions explicit and prevents illegal jumps (e.g. `REJECTED → VERIFIED`).

**How.** `KycCase` holds a `status`. Document submission stores a reference and
fires async vendor orchestration (the document bytes go to object storage; only
a pointer is in PostgreSQL). The vendor callback (or polling worker) transitions
the case. `KycGate` is consulted by `TransferService` before any money-out.

```java
public enum KycStatus { CREATED, DOCS_SUBMITTED, UNDER_REVIEW, VERIFIED, REJECTED }

@Entity @Table(name = "kyc_cases")
public class KycCase {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false, unique = true) private UUID accountId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private KycStatus status;

    @Column private String vendorRef;      // external verification id
    @Column private String rejectReason;
    @Column(nullable = false) private Instant updatedAt;

    public void transitionTo(KycStatus next) {
        if (!ALLOWED.get(this.status).contains(next)) {
            throw new ApiException("KYC_ILLEGAL_TRANSITION",
                "%s -> %s not allowed".formatted(status, next), 409);
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }

    private static final Map<KycStatus, Set<KycStatus>> ALLOWED = Map.of(
        KycStatus.CREATED,        EnumSet.of(KycStatus.DOCS_SUBMITTED),
        KycStatus.DOCS_SUBMITTED, EnumSet.of(KycStatus.UNDER_REVIEW),
        KycStatus.UNDER_REVIEW,   EnumSet.of(KycStatus.VERIFIED, KycStatus.REJECTED),
        KycStatus.VERIFIED,       EnumSet.noneOf(KycStatus.class),
        KycStatus.REJECTED,       EnumSet.of(KycStatus.DOCS_SUBMITTED));   // allow re-submit
}
```

### Document submission + async vendor orchestration

```java
@Service
public class DefaultKycService implements KycService {

    private final KycCaseRepository cases;
    private final DocumentStore docStore;
    private final KycVendorClient vendor;

    // constructor injection omitted

    @Transactional
    public void submitDocument(UUID accountId, DocumentUpload upload) {
        KycCase kyc = cases.findByAccountId(accountId).orElseThrow();
        String ref = docStore.put(upload.bytes(), upload.contentType());  // object storage
        kyc.transitionTo(KycStatus.DOCS_SUBMITTED);
        // Hand off asynchronously — do NOT block the request on the vendor.
        orchestrateAsync(kyc.getId());
    }

    @Async
    public void orchestrateAsync(UUID caseId) {
        cases.findById(caseId).ifPresent(kyc -> {
            kyc.transitionTo(KycStatus.UNDER_REVIEW);
            cases.save(kyc);
            VendorResult r = vendor.verify(kyc.getAccountId());   // network call off the hot path
            applyVendorResult(caseId, r);
        });
    }

    @Transactional
    public void applyVendorResult(UUID caseId, VendorResult r) {
        KycCase kyc = cases.findById(caseId).orElseThrow();
        kyc.setVendorRef(r.reference());
        kyc.transitionTo(r.approved() ? KycStatus.VERIFIED : KycStatus.REJECTED);
        if (!r.approved()) kyc.setRejectReason(r.reason());
        cases.save(kyc);
    }
}
```

### KYC gate consulted by money-out

```java
@Component
public class KycGate {

    private final KycCaseRepository cases;

    public KycGate(KycCaseRepository cases) { this.cases = cases; }

    public void assertMoneyOutAllowed(UUID accountId) {
        KycStatus status = cases.findByAccountId(accountId)
            .map(KycCase::getStatus).orElse(KycStatus.CREATED);
        if (status != KycStatus.VERIFIED) {
            throw new ApiException("KYC_REQUIRED",
                "Account KYC not verified (" + status + ")", 403);
        }
    }
}
```

---

## 7. Audit Trail

**What.** An **append-only** `audit_records` table capturing actor, action,
before/after snapshots, and the `trace_id` of the request. Privileged actions
write an audit record; there is a query endpoint for auditors.

**Why.** Auditability is a regulatory requirement and an incident-response tool.
We want a tamper-evident "who did what, when, and what changed" record that is
independent of application logs (which rotate and are mutable). Writing it
inside the business transaction means an action and its audit entry commit
together.

**How.** A lightweight AOP aspect intercepts methods annotated `@Audited` and
writes an `AuditRecord`; high-stakes paths also call the `AuditService`
explicitly with rich before/after payloads. The `trace_id` is pulled from the
current OpenTelemetry span.

```java
@Entity @Table(name = "audit_records")   // append-only
public class AuditRecord {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false) private String actor;      // user id or "system"
    @Column(nullable = false) private String action;     // e.g. "user:assign-role"
    @Column(columnDefinition = "jsonb") private String beforeJson;
    @Column(columnDefinition = "jsonb") private String afterJson;
    @Column(nullable = false) private String traceId;
    @Column(nullable = false) private Instant at;
    // accessors omitted
}
```

```java
@Aspect @Component
public class AuditAspect {

    private final AuditService audit;

    public AuditAspect(AuditService audit) { this.audit = audit; }

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void record(JoinPoint jp, Audited audited, Object result) {
        audit.write(currentActor(), audited.action(), null, result);
    }

    private String currentActor() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null ? a.getName() : "system";
    }
}
```

```java
@Service
public class DefaultAuditService implements AuditService {

    private final AuditRecordRepository repo;
    private final ObjectMapper mapper;
    private final Tracer tracer;   // OpenTelemetry

    // constructor injection omitted

    @Transactional(propagation = Propagation.MANDATORY)   // must join caller's TX
    public void write(String actor, String action, Object before, Object after) {
        AuditRecord r = new AuditRecord();
        r.setActor(actor);
        r.setAction(action);
        r.setBeforeJson(toJson(before));
        r.setAfterJson(toJson(after));
        r.setTraceId(Span.current().getSpanContext().getTraceId());
        r.setAt(Instant.now());
        repo.save(r);
    }

    private String toJson(Object o) {
        try { return o == null ? null : mapper.writeValueAsString(o); }
        catch (JsonProcessingException e) { return "\"<unserializable>\""; }
    }
}
```

### Query endpoint

```java
@RestController
@RequestMapping("/v1/audit")
public class AuditController {

    private final AuditRecordRepository repo;

    public AuditController(AuditRecordRepository repo) { this.repo = repo; }

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")     // auditor role only
    public Page<AuditRecord> search(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            Pageable pageable) {
        return repo.search(actor, action, pageable);
    }
}
```

---

## 8. Event-Driven / Transactional Outbox

**What.** Domain changes that need to fan out (notifications, projections,
analytics) are recorded as an **`OutboxEvent`** written *inside the same DB
transaction* as the change. A `@Scheduled` **relay** reads unpublished rows and
publishes them to RabbitMQ. Consumers are **idempotent**, deduping by event id.

**Why.** The classic failure is "dual write": commit the DB row, then publish to
the broker — and crash in between, losing the event (or publishing then failing
the DB commit, emitting a phantom event). The outbox makes the event part of the
ACID transaction, so it is published **exactly when** the change is durable. The
relay provides at-least-once delivery; idempotent consumers make at-least-once
behave like exactly-once. **Money is never moved by events** — only side-effects.

**How.** `OutboxWriter.write(...)` is called within `TransferService.transfer`
(§5). The relay polls, marks rows in-flight, publishes, and marks published.
Consumers record processed event ids and skip duplicates.

```java
@Entity @Table(name = "outbox_events")
public class OutboxEvent {
    @Id @GeneratedValue private UUID id;            // also the dedup key for consumers
    @Column(nullable = false) private String type;  // "transfer.completed"
    @Column(nullable = false) private String aggregateId;
    @Column(nullable = false, columnDefinition = "jsonb") private String payload;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private OutboxStatus status;                     // NEW, PUBLISHED

    @Column(nullable = false) private Instant createdAt;
    @Column private Instant publishedAt;
    // accessors omitted
}
```

### Writing the outbox row inside the money transaction

```java
@Component
public class OutboxWriter {

    private final OutboxEventRepository repo;
    private final ObjectMapper mapper;

    public OutboxWriter(OutboxEventRepository repo, ObjectMapper mapper) {
        this.repo = repo; this.mapper = mapper;
    }

    // No @Transactional here: it MUST run inside the caller's transaction (transfer).
    public void write(String type, UUID aggregateId, Map<String, Object> payload) {
        OutboxEvent e = new OutboxEvent();
        e.setType(type);
        e.setAggregateId(aggregateId.toString());
        e.setPayload(serialize(payload));
        e.setStatus(OutboxStatus.NEW);
        e.setCreatedAt(Instant.now());
        repo.save(e);   // committed atomically with the ledger entries
    }

    private String serialize(Map<String, Object> p) {
        try { return mapper.writeValueAsString(p); }
        catch (JsonProcessingException ex) { throw new IllegalStateException(ex); }
    }
}
```

### The @Scheduled relay

```java
@Component
public class OutboxRelay {

    private final OutboxEventRepository repo;
    private final RabbitTemplate rabbit;

    public OutboxRelay(OutboxEventRepository repo, RabbitTemplate rabbit) {
        this.repo = repo; this.rabbit = rabbit;
    }

    @Scheduled(fixedDelay = 1000)   // poll every second
    @Transactional
    public void publishBatch() {
        List<OutboxEvent> batch = repo.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.NEW);
        for (OutboxEvent e : batch) {
            // event id rides as a message property so consumers can dedup.
            rabbit.convertAndSend("bank.events", e.getType(), e.getPayload(),
                m -> { m.getMessageProperties().setMessageId(e.getId().toString());
                       m.getMessageProperties().setContentType("application/json");
                       return m; });
            e.setStatus(OutboxStatus.PUBLISHED);
            e.setPublishedAt(Instant.now());
        }
    }
}
```

### Idempotent consumer

```java
@Component
public class TransferNotificationConsumer {

    private final ProcessedEventRepository processed;
    private final NotificationSender notifier;

    // constructor injection omitted

    @RabbitListener(queues = "notifications.transfer-completed")
    @Transactional
    public void onTransferCompleted(Message msg) {
        UUID eventId = UUID.fromString(msg.getMessageProperties().getMessageId());

        // Dedup: insert-first on a unique PK; duplicate => already handled, ack & skip.
        if (processed.existsById(eventId)) return;
        try {
            processed.saveAndFlush(new ProcessedEvent(eventId, Instant.now()));
        } catch (DataIntegrityViolationException dup) {
            return;   // concurrent duplicate
        }
        notifier.sendTransferReceipt(new String(msg.getBody(), StandardCharsets.UTF_8));
    }
}
```

RabbitMQ topology declared once at startup:

```java
@Configuration
public class RabbitConfig {

    @Bean TopicExchange eventsExchange() { return new TopicExchange("bank.events"); }

    @Bean Queue transferQueue() {
        return QueueBuilder.durable("notifications.transfer-completed")
            .deadLetterExchange("bank.events.dlx")   // poison messages park here
            .build();
    }

    @Bean Binding transferBinding(Queue transferQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(transferQueue).to(eventsExchange).with("transfer.completed");
    }
}
```

---

## 9. API Gateway & Rate Limiting

**What.** A separate **API gateway** sits in front of the app (TLS termination,
routing, coarse rate limiting / WAF). Inside the application we keep a
**per-principal rate limiter** (Bucket4j) as defense in depth, a standard
**error envelope** `{code, message, trace_id}`, and a global
`@ControllerAdvice` exception handler.

**Why.** The gateway is the right place for network-edge concerns, but the app
should not trust that it is always there — a second, application-aware limiter
protects expensive endpoints (e.g. transfers) even if edge config drifts. A
single error envelope means every client parses errors the same way, and the
`trace_id` ties a client-visible error to server traces and audit records.

**How.** A `OncePerRequestFilter` keyed by the authenticated subject consumes a
token bucket. The exception handler maps `ApiException` and validation errors to
the envelope and stamps the current trace id.

### Error envelope

```java
public record ApiError(String code, String message, String traceId) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Span.current().getSpanContext().getTraceId());
    }
}
```

### Global exception handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
            .body(ApiError.of(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + " " + f.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiError.of("VALIDATION", msg));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiError.of("FORBIDDEN", "Permission denied"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        // never leak internals; trace_id lets support correlate to logs
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError.of("INTERNAL", "Unexpected error"));
    }
}
```

### Bucket4j rate-limit filter

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
            .build();   // 100 requests / minute / principal
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String key = principalKey(req);
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("""
                {"code":"RATE_LIMITED","message":"Too many requests","trace_id":"%s"}"""
                .formatted(Span.current().getSpanContext().getTraceId()));
        }
    }

    private String principalKey(HttpServletRequest req) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : req.getRemoteAddr();
    }
}
```

---

## 10. Observability

**What.** Spring Boot **Actuator** exposes health and Prometheus-format metrics
via **Micrometer**; **OpenTelemetry** provides distributed tracing with the
`trace_id` propagated through logs, the error envelope, and audit records. We
add custom business counters (e.g. transfers posted, idempotent replays).

**Why.** A money system must be observable in production: we need to know
transfer throughput, failure rates, outbox lag, and be able to follow one
request end-to-end. Business metrics (not just CPU/memory) are what tell us the
platform is healthy.

**How.** Actuator endpoints are exposed selectively. A `MeterRegistry`-backed
counter is incremented in `TransferService`. Trace context is auto-propagated by
the OpenTelemetry Spring Boot starter; we just read the current trace id where
needed.

### application.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      probes:
        enabled: true        # liveness/readiness for Kubernetes
  metrics:
    tags:
      application: bank-core
  tracing:
    sampling:
      probability: 1.0       # sample all in non-prod; lower in prod
```

### Custom counter

```java
@Service
public class TransferMetrics {

    private final Counter posted;
    private final Counter replayed;
    private final Counter insufficientFunds;

    public TransferMetrics(MeterRegistry registry) {
        this.posted = Counter.builder("bank.transfers.posted")
            .description("Successfully posted transfers").register(registry);
        this.replayed = Counter.builder("bank.transfers.replayed")
            .description("Idempotent replays").register(registry);
        this.insufficientFunds = Counter.builder("bank.transfers.rejected")
            .tag("reason", "insufficient_funds").register(registry);
    }

    public void posted()            { posted.increment(); }
    public void replayed()          { replayed.increment(); }
    public void insufficientFunds() { insufficientFunds.increment(); }
}
```

**Tracing note.** Each inbound request begins (or continues) a trace; the
gateway injects W3C `traceparent` headers, the OpenTelemetry starter creates a
server span, and downstream RabbitMQ publishes propagate the context so an event
consumed minutes later is still linked to the originating transfer. The same
`trace_id` is what surfaces in `ApiError`, `AuditRecord`, and structured logs —
one id correlates the whole story.

---

## 11. Testing Note — Invariants

**What.** Tests that prove the two non-negotiable properties: **double-entry
balances to zero** and **idempotency prevents double-posting**.

**Why.** These are the invariants the whole platform rests on. They must be
asserted by automated tests, not assumed. A double-entry test that sums all
legs of a transaction and expects zero will catch any future change that breaks
balance. An idempotency test proves a retried request never moves money twice.

**How.** A Spring Boot integration test (Testcontainers PostgreSQL) runs a real
transfer, then asserts (1) the two ledger legs net to zero, and (2) a second
call with the same idempotency key produces no new entries and returns the
original transaction.

```java
@SpringBootTest
@Testcontainers
class TransferInvariantTest {

    @Container
    static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", db::getJdbcUrl);
        r.add("spring.datasource.username", db::getUsername);
        r.add("spring.datasource.password", db::getPassword);
    }

    @Autowired TransferService transfers;
    @Autowired LedgerEntryRepository ledger;
    @Autowired TestFixtures fx;   // seeds verified accounts + funded wallets

    @Test
    void doubleEntry_balancesToZero() {
        var cmd = fx.transferCommand("100.00");
        transfers.transfer(cmd);

        List<LedgerEntry> legs = ledger.findByTransactionId(cmd.transactionId());
        assertThat(legs).hasSize(2);

        BigDecimal net = legs.stream()
            .map(e -> e.getDirection() == Direction.CREDIT ? e.getAmount() : e.getAmount().negate())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(net).isEqualByComparingTo("0.00");   // debits == credits
    }

    @Test
    void idempotencyKey_preventsDoublePost() {
        var cmd = fx.transferCommand("50.00");

        TransferResult first  = transfers.transfer(cmd);
        TransferResult second = transfers.transfer(cmd);   // same idempotency key

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.transactionId()).isEqualTo(first.transactionId());

        // Only ONE transaction's worth of legs exists.
        assertThat(ledger.findByTransactionId(cmd.transactionId())).hasSize(2);
    }

    @Test
    void insufficientFunds_isRejected() {
        var cmd = fx.transferCommand("1000000.00");   // exceeds funded balance
        assertThatThrownBy(() -> transfers.transfer(cmd))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("INSUFFICIENT_FUNDS");
    }
}
```

---

## Summary

The backend is a Spring Boot 3 modular monolith where **money is correct by
construction**: every movement is a balanced, append-only, double-entry posting
committed in one ACID transaction, protected by DB-enforced idempotency and a
KYC gate. **Stateless RS256 JWTs** with rotating, reuse-detecting refresh tokens
secure access, and **default-deny RBAC** binds endpoints to permissions.
**Side-effects** flow through a **transactional outbox** to RabbitMQ with
idempotent consumers — never moving money. An **append-only audit trail**,
**Actuator/Micrometer metrics**, **OpenTelemetry tracing**, and a standard
`{code, message, trace_id}` error envelope make the system auditable and
observable end-to-end.

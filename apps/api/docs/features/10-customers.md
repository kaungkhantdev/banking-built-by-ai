# Feature 10 — Customer Management

**Package:** `com.bank.feature.customers` · **Endpoints:** `/v1/customers/*`

## Purpose
Manage the customer entity that owns one or more accounts. A customer holds personal
information, has a lifecycle status, and is the root identity for KYC and compliance checks.

## Layout
```
customers/
├── web/        CustomerController, dto/ (CreateCustomerRequest, UpdateCustomerRequest, CustomerView)
├── domain/     CustomerService + DefaultCustomerService, CustomerStatus
└── persistence/ Customer, CustomerRepository
```

## Lifecycle
`ACTIVE → SUSPENDED → CLOSED`. New customers start `ACTIVE` upon profile creation.
Suspended customers cannot initiate transfers.

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| POST | `/v1/customers` | `customer:create` | Create a customer profile |
| GET | `/v1/customers/{id}` | `customer:read` | Retrieve a customer |
| PATCH | `/v1/customers/{id}` | `customer:update` | Update personal information |
| GET | `/v1/customers` | `customer:read` | Search customers (paginated) |

## Key rules
- Each customer profile update writes an audit record via `@Audited`.
- Status transitions are guarded; illegal transitions return `409`.
- Search supports filtering by name, email, phone, and status with cursor-based pagination.

## Why
Separating the customer entity from the auth `User` and the `Account` allows one customer
to hold multiple accounts and lets compliance/KYC reference a single canonical identity.

## Related requirements
FR-10.*

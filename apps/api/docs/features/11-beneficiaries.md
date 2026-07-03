# Feature 11 — Beneficiaries

**Package:** `com.bank.feature.beneficiaries` · **Endpoints:** `/v1/beneficiaries/*`

## Purpose
Allow users to save and reuse trusted transfer destinations (beneficiaries) so they
do not need to re-enter wallet or account details on every transfer.

## Layout
```
beneficiaries/
├── web/        BeneficiaryController, dto/ (CreateBeneficiaryRequest, BeneficiaryView)
├── domain/     BeneficiaryService + DefaultBeneficiaryService
└── persistence/ Beneficiary, BeneficiaryRepository
```

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| POST | `/v1/beneficiaries` | `beneficiary:create` | Save a new beneficiary |
| GET | `/v1/beneficiaries` | `beneficiary:read` | List saved beneficiaries |
| PATCH | `/v1/beneficiaries/{id}` | `beneficiary:update` | Update a beneficiary |
| DELETE | `/v1/beneficiaries/{id}` | `beneficiary:delete` | Remove a beneficiary |

## Key rules
- A unique constraint on `(owner_user_id, destination_wallet_id)` prevents duplicates;
  violations return `409 BENEFICIARY_DUPLICATE`.
- Transfers that reference a beneficiary id resolve the destination wallet automatically.
- Deleting a beneficiary does not affect historical transfer records.

## Why
Reduces user friction for repeat payments and reduces the risk of typos in destination
wallet identifiers for high-value transfers.

## Related requirements
FR-11.*

-- V6: seed RBAC roles + permissions (idempotent inserts).
-- Permission names match the @PreAuthorize("hasAuthority('...')") checks in code.

INSERT INTO permissions (id, name) VALUES
    (gen_random_uuid(), 'account:create'),
    (gen_random_uuid(), 'account:manage'),
    (gen_random_uuid(), 'wallet:create'),
    (gen_random_uuid(), 'wallet:read'),
    (gen_random_uuid(), 'wallet:manage'),
    (gen_random_uuid(), 'transfer:create'),
    (gen_random_uuid(), 'transaction:reverse'),
    (gen_random_uuid(), 'kyc:submit'),
    (gen_random_uuid(), 'kyc:read'),
    (gen_random_uuid(), 'kyc:override'),
    (gen_random_uuid(), 'audit:read'),
    (gen_random_uuid(), 'user:assign-role')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (id, name) VALUES
    (gen_random_uuid(), 'CUSTOMER'),
    (gen_random_uuid(), 'OPERATOR'),
    (gen_random_uuid(), 'AUDITOR')
ON CONFLICT (name) DO NOTHING;

-- CUSTOMER: operate own wallets + KYC + transfers.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CUSTOMER'
  AND p.name IN ('wallet:create','wallet:read','transfer:create','kyc:submit','kyc:read')
ON CONFLICT DO NOTHING;

-- OPERATOR: customer/account/wallet management + reversals + role assignment.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'OPERATOR'
  AND p.name IN ('account:create','account:manage','wallet:read','wallet:manage',
                 'transaction:reverse','kyc:read','kyc:override','user:assign-role')
ON CONFLICT DO NOTHING;

-- AUDITOR: read the audit trail only.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'AUDITOR'
  AND p.name IN ('audit:read')
ON CONFLICT DO NOTHING;

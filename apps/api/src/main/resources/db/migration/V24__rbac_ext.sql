-- V24: new permissions for features 10-28
INSERT INTO permissions (id, name) VALUES
    (gen_random_uuid(), 'customer:create'),
    (gen_random_uuid(), 'customer:read'),
    (gen_random_uuid(), 'customer:update'),
    (gen_random_uuid(), 'beneficiary:create'),
    (gen_random_uuid(), 'beneficiary:read'),
    (gen_random_uuid(), 'beneficiary:update'),
    (gen_random_uuid(), 'beneficiary:delete'),
    (gen_random_uuid(), 'account:read'),
    (gen_random_uuid(), 'transfer:read'),
    (gen_random_uuid(), 'report:generate'),
    (gen_random_uuid(), 'report:read'),
    (gen_random_uuid(), 'webhook:manage'),
    (gen_random_uuid(), 'file:manage'),
    (gen_random_uuid(), 'admin:read'),
    (gen_random_uuid(), 'admin:manage')
ON CONFLICT (name) DO NOTHING;

-- CUSTOMER: own profile, beneficiaries, transfer history, webhooks, files
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CUSTOMER'
  AND p.name IN (
    'customer:create','customer:read','customer:update',
    'beneficiary:create','beneficiary:read','beneficiary:update','beneficiary:delete',
    'account:read','transfer:read','webhook:manage','file:manage'
  )
ON CONFLICT DO NOTHING;

-- OPERATOR: customer oversight, admin, reports, files
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'OPERATOR'
  AND p.name IN (
    'customer:read','customer:update',
    'admin:read','admin:manage',
    'report:generate','report:read','file:manage'
  )
ON CONFLICT DO NOTHING;

-- AUDITOR: read-only access to reports and admin views
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'AUDITOR'
  AND p.name IN ('report:read','admin:read')
ON CONFLICT DO NOTHING;

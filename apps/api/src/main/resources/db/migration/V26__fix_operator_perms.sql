-- V26: grant OPERATOR the permissions it was missing for dashboard pages.
-- account:read   → Accounts page, Statement endpoints
-- transfer:read  → Transfers & Scheduled Transfers pages
-- audit:read     → Audit Trail page
-- webhook:manage → Webhooks admin page

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'OPERATOR'
  AND p.name IN ('account:read', 'transfer:read', 'audit:read', 'webhook:manage')
ON CONFLICT DO NOTHING;

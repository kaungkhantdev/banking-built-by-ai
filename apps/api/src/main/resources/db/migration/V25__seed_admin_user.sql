-- V25: bootstrap admin user (OPERATOR role).
-- Email: admin@bank.local  Password: Admin1234!
-- Change the password immediately after first login in production.

INSERT INTO users (id, email, password_hash, enabled, created_at, updated_at, version)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@bank.local',
    '$2a$12$Bh8nzp1NHT0SpFA/bL6Cfer9sY6xvAAORQ2aWGf0xdqu2sMrIHWe2',
    TRUE,
    NOW(),
    NOW(),
    0
) ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (id, user_id, role_id)
SELECT
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    r.id
FROM roles r
WHERE r.name = 'OPERATOR'
ON CONFLICT DO NOTHING;

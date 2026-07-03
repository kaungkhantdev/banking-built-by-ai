-- V27: seed demo data for dashboard and pages to show realistic content.
-- Users password: Password1!  (bcrypt cost 12)

-- ── Users ─────────────────────────────────────────────────────────────────────

INSERT INTO users (id, email, password_hash, enabled, created_at, updated_at, version) VALUES
    ('10000000-0000-0000-0000-000000000001', 'alice@demo.com',
     '$2a$12$90cl8EMCJN5PQKV6gNRype5/p.jfWo2EDsx4/Ig68CVmvf/HlrG4W',
     TRUE, NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', 0),
    ('10000000-0000-0000-0000-000000000002', 'bob@demo.com',
     '$2a$12$90cl8EMCJN5PQKV6gNRype5/p.jfWo2EDsx4/Ig68CVmvf/HlrG4W',
     TRUE, NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days', 0),
    ('10000000-0000-0000-0000-000000000003', 'carol@demo.com',
     '$2a$12$90cl8EMCJN5PQKV6gNRype5/p.jfWo2EDsx4/Ig68CVmvf/HlrG4W',
     TRUE, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days', 0),
    ('10000000-0000-0000-0000-000000000004', 'auditor@bank.local',
     '$2a$12$90cl8EMCJN5PQKV6gNRype5/p.jfWo2EDsx4/Ig68CVmvf/HlrG4W',
     TRUE, NOW() - INTERVAL '60 days', NOW() - INTERVAL '60 days', 0)
ON CONFLICT (email) DO NOTHING;

-- ── Roles ─────────────────────────────────────────────────────────────────────

INSERT INTO user_roles (id, user_id, role_id)
SELECT gen_random_uuid(), u.id, r.id FROM users u, roles r
WHERE u.email IN ('alice@demo.com','bob@demo.com','carol@demo.com') AND r.name = 'CUSTOMER'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (id, user_id, role_id)
SELECT gen_random_uuid(), u.id, r.id FROM users u, roles r
WHERE u.email = 'auditor@bank.local' AND r.name = 'AUDITOR'
ON CONFLICT DO NOTHING;

-- ── Accounts ──────────────────────────────────────────────────────────────────

INSERT INTO accounts (id, owner_user_id, status, created_at, updated_at, version) VALUES
    ('20000000-0000-0000-0000-000000000001',
     '10000000-0000-0000-0000-000000000001', 'ACTIVE',
     NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', 0),
    ('20000000-0000-0000-0000-000000000002',
     '10000000-0000-0000-0000-000000000002', 'ACTIVE',
     NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days', 0),
    ('20000000-0000-0000-0000-000000000003',
     '10000000-0000-0000-0000-000000000003', 'PENDING',
     NOW() - INTERVAL '5 days',  NOW() - INTERVAL '5 days',  0)
ON CONFLICT DO NOTHING;

-- ── Wallets ───────────────────────────────────────────────────────────────────

INSERT INTO wallets (id, account_id, currency, status, created_at, updated_at, version) VALUES
    ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'THB', 'ACTIVE', NOW() - INTERVAL '30 days', NOW(), 0),
    ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'USD', 'ACTIVE', NOW() - INTERVAL '30 days', NOW(), 0),
    ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002', 'THB', 'ACTIVE', NOW() - INTERVAL '20 days', NOW(), 0),
    ('30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000002', 'USD', 'ACTIVE', NOW() - INTERVAL '20 days', NOW(), 0),
    ('30000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000003', 'THB', 'ACTIVE', NOW() - INTERVAL '5 days',  NOW(), 0)
ON CONFLICT DO NOTHING;

-- ── Ledger entries (5 transactions, double-entry each) ─────────────────────────

-- TX1: Alice receives 50,000 THB (opening deposit)
INSERT INTO ledger_entries (id, transaction_id, wallet_id, direction, amount, currency, posted_at, memo) VALUES
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001',
     '30000000-0000-0000-0000-000000000001', 'CREDIT', 50000.0000, 'THB',
     NOW() - INTERVAL '29 days', 'Opening deposit'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001',
     '30000000-0000-0000-0000-000000000003', 'DEBIT',  50000.0000, 'THB',
     NOW() - INTERVAL '29 days', 'Opening deposit')
ON CONFLICT DO NOTHING;

-- TX2: Bob receives 30,000 THB
INSERT INTO ledger_entries (id, transaction_id, wallet_id, direction, amount, currency, posted_at, memo) VALUES
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002',
     '30000000-0000-0000-0000-000000000003', 'CREDIT', 30000.0000, 'THB',
     NOW() - INTERVAL '19 days', 'Opening deposit'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002',
     '30000000-0000-0000-0000-000000000001', 'DEBIT',  30000.0000, 'THB',
     NOW() - INTERVAL '19 days', 'Opening deposit')
ON CONFLICT DO NOTHING;

-- TX3: Alice sends 5,000 THB to Bob
INSERT INTO ledger_entries (id, transaction_id, wallet_id, direction, amount, currency, posted_at, memo) VALUES
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000003',
     '30000000-0000-0000-0000-000000000001', 'DEBIT',  5000.0000, 'THB',
     NOW() - INTERVAL '15 days', 'Rent payment'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000003',
     '30000000-0000-0000-0000-000000000003', 'CREDIT', 5000.0000, 'THB',
     NOW() - INTERVAL '15 days', 'Rent payment')
ON CONFLICT DO NOTHING;

-- TX4: Alice USD deposit
INSERT INTO ledger_entries (id, transaction_id, wallet_id, direction, amount, currency, posted_at, memo) VALUES
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000004',
     '30000000-0000-0000-0000-000000000002', 'CREDIT', 1000.0000, 'USD',
     NOW() - INTERVAL '10 days', 'Wire transfer'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000004',
     '30000000-0000-0000-0000-000000000004', 'DEBIT',  1000.0000, 'USD',
     NOW() - INTERVAL '10 days', 'Wire transfer')
ON CONFLICT DO NOTHING;

-- TX5: Bob sends 2,000 THB to Carol
INSERT INTO ledger_entries (id, transaction_id, wallet_id, direction, amount, currency, posted_at, memo) VALUES
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000005',
     '30000000-0000-0000-0000-000000000003', 'DEBIT',  2000.0000, 'THB',
     NOW() - INTERVAL '3 days', 'Invoice #1042'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000005',
     '30000000-0000-0000-0000-000000000005', 'CREDIT', 2000.0000, 'THB',
     NOW() - INTERVAL '3 days', 'Invoice #1042')
ON CONFLICT DO NOTHING;

-- ── KYC cases ─────────────────────────────────────────────────────────────────

INSERT INTO kyc_cases (id, account_id, status, updated_at) VALUES
    ('40000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000001', 'VERIFIED',      NOW() - INTERVAL '25 days'),
    ('40000000-0000-0000-0000-000000000002',
     '20000000-0000-0000-0000-000000000002', 'UNDER_REVIEW',  NOW() - INTERVAL '18 days'),
    ('40000000-0000-0000-0000-000000000003',
     '20000000-0000-0000-0000-000000000003', 'CREATED',       NOW() - INTERVAL '5 days')
ON CONFLICT DO NOTHING;

-- V31: broaden demo data so every dashboard/app page shows realistic content.
-- Builds on V27 (users 1000…0001-0004, accounts 2000…, wallets 3000…).
-- Idempotent: fixed ids + ON CONFLICT DO NOTHING. Transient/internal tables
-- (outbox_events, processed_events, idempotency_keys, refresh_tokens, mfa_*,
-- reset tokens) are intentionally NOT seeded.

-- ── Customers (Feature 10) ────────────────────────────────────────────────────
INSERT INTO customers (id, user_id, full_name, phone, date_of_birth, status, created_at, updated_at, version) VALUES
    ('c1000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Alice Andrews', '+66801112233', '1990-04-12', 'ACTIVE',    NOW() - INTERVAL '30 days', NOW(), 0),
    ('c1000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'Bob Brown',     '+66802223344', '1985-11-03', 'ACTIVE',    NOW() - INTERVAL '20 days', NOW(), 0),
    ('c1000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 'Carol Chen',    '+66803334455', '1998-07-21', 'SUSPENDED', NOW() - INTERVAL '10 days', NOW(), 0)
ON CONFLICT DO NOTHING;

-- ── Beneficiaries (Feature 11) ────────────────────────────────────────────────
INSERT INTO beneficiaries (id, owner_user_id, alias, destination_wallet_id, created_at, updated_at, version) VALUES
    ('b1000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Bob (THB)',   '30000000-0000-0000-0000-000000000003', NOW() - INTERVAL '18 days', NOW(), 0),
    ('b1000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Carol (THB)', '30000000-0000-0000-0000-000000000005', NOW() - INTERVAL '9 days',  NOW(), 0),
    ('b1000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'Alice (THB)', '30000000-0000-0000-0000-000000000001', NOW() - INTERVAL '12 days', NOW(), 0)
ON CONFLICT DO NOTHING;

-- ── Exchange rates (Feature 18) ───────────────────────────────────────────────
INSERT INTO exchange_rates (id, from_currency, to_currency, rate, effective_at, source, created_at) VALUES
    ('ec000000-0000-0000-0000-000000000001', 'USD', 'THB', 36.50000000, NOW() - INTERVAL '2 days',  'seed', NOW() - INTERVAL '2 days'),
    ('ec000000-0000-0000-0000-000000000002', 'USD', 'THB', 36.62000000, NOW() - INTERVAL '1 days',  'seed', NOW() - INTERVAL '1 days'),
    ('ec000000-0000-0000-0000-000000000003', 'USD', 'THB', 36.48000000, NOW(),                      'seed', NOW()),
    ('ec000000-0000-0000-0000-000000000004', 'USD', 'EUR', 0.92000000,  NOW(),                      'seed', NOW()),
    ('ec000000-0000-0000-0000-000000000005', 'USD', 'GBP', 0.79000000,  NOW(),                      'seed', NOW()),
    ('ec000000-0000-0000-0000-000000000006', 'EUR', 'USD', 1.08700000,  NOW(),                      'seed', NOW()),
    ('ec000000-0000-0000-0000-000000000007', 'THB', 'USD', 0.02740000,  NOW(),                      'seed', NOW())
ON CONFLICT DO NOTHING;

-- ── Fee config + promotional waiver (Feature 19) ──────────────────────────────
INSERT INTO fee_configs (id, transfer_type, customer_tier, fee_type, fee_value, min_fee, max_fee, active, created_at, updated_at, version) VALUES
    ('fee00000-0000-0000-0000-000000000001', 'STANDARD', 'STANDARD', 'PERCENTAGE', 0.5000, 5.0000, 100.0000, TRUE, NOW() - INTERVAL '40 days', NOW(), 0)
ON CONFLICT DO NOTHING;

INSERT INTO waiver_records (id, user_id, fee_config_id, reason, valid_from, valid_until, created_at) VALUES
    ('fab00000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'fee00000-0000-0000-0000-000000000001', 'New-customer promo', NOW() - INTERVAL '5 days', NOW() + INTERVAL '25 days', NOW() - INTERVAL '5 days')
ON CONFLICT DO NOTHING;

-- ── Scheduled transfers (Feature 25) ──────────────────────────────────────────
INSERT INTO scheduled_transfers (id, owner_user_id, from_wallet_id, to_wallet_id, amount, memo, recurrence_rule, next_run_at, status, attempt_count, max_attempts, last_error, last_run_at, created_at, updated_at, version) VALUES
    ('5c000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000003', 1000.0000, 'Monthly rent', 'MONTHLY', NOW() + INTERVAL '3 days',  'ACTIVE',    0, 3, NULL, NOW() - INTERVAL '27 days', NOW() - INTERVAL '30 days', NOW(), 0),
    ('5c000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000005', 500.0000,  'Weekly savings','WEEKLY', NOW() + INTERVAL '2 days',  'ACTIVE',    0, 3, NULL, NOW() - INTERVAL '2 days',  NOW() - INTERVAL '20 days', NOW(), 0),
    ('5c000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000001', 9999.0000, 'One-off',      NULL,      NOW() - INTERVAL '1 days',  'FAILED',    3, 3, 'INSUFFICIENT_FUNDS', NOW() - INTERVAL '1 days', NOW() - INTERVAL '4 days', NOW(), 0)
ON CONFLICT DO NOTHING;

-- ── Reports (Feature 24) ──────────────────────────────────────────────────────
INSERT INTO report_records (id, report_type, from_date, to_date, format, status, file_key, filters, requested_by, created_at, updated_at, version) VALUES
    ('6e000000-0000-0000-0000-000000000001', 'TRANSACTION_SUMMARY', DATE '2026-06-01', DATE '2026-06-30', 'CSV',  'READY',   'reports/6e000000-0000-0000-0000-000000000001.csv', NULL, '10000000-0000-0000-0000-000000000004', NOW() - INTERVAL '3 days', NOW(), 0),
    ('6e000000-0000-0000-0000-000000000002', 'TRANSACTION_SUMMARY', DATE '2026-07-01', DATE '2026-07-03', 'XLSX', 'PENDING', NULL,                                              NULL, '10000000-0000-0000-0000-000000000004', NOW() - INTERVAL '1 hours', NOW(), 0)
ON CONFLICT DO NOTHING;

-- ── Statements (Feature 17) — digest NULL so download regenerates without a tamper check
INSERT INTO statement_records (id, account_id, period_year, period_month, status, file_key, digest, created_at, updated_at, version) VALUES
    ('57000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 2026, 5, 'READY', 'statements/57000000-0000-0000-0000-000000000001.pdf', NULL, NOW() - INTERVAL '35 days', NOW(), 0),
    ('57000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 2026, 6, 'READY', 'statements/57000000-0000-0000-0000-000000000002.pdf', NULL, NOW() - INTERVAL '4 days',  NOW(), 0)
ON CONFLICT DO NOTHING;

-- ── Fraud alerts (Feature 22) — a mix so the review page filters are meaningful
INSERT INTO fraud_alerts (id, transfer_id, user_id, risk_score, status, rule_details, created_at, updated_at, version) VALUES
    ('fa000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000002', 85, 'BLOCKED',        'HIGH_VALUE:9999',           NOW() - INTERVAL '2 days',  NOW(), 0),
    ('fa000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001', 55, 'PENDING_REVIEW', 'VELOCITY:11/60m',           NOW() - INTERVAL '1 days',  NOW(), 0),
    ('fa000000-0000-0000-0000-000000000003', NULL,                                    '20000000-0000-0000-0000-000000000002', 90, 'APPROVED',       'HIGH_VALUE, VELOCITY',      NOW() - INTERVAL '6 days',  NOW(), 0),
    ('fa000000-0000-0000-0000-000000000004', NULL,                                    '20000000-0000-0000-0000-000000000001', 50, 'FALSE_POSITIVE', 'HIGH_VALUE:5200',           NOW() - INTERVAL '8 days',  NOW(), 0)
ON CONFLICT DO NOTHING;

-- ── Compliance decisions (Feature 23) ─────────────────────────────────────────
INSERT INTO compliance_decisions (id, entity_type, entity_id, rule_version, decision, details, created_at) VALUES
    ('cd000000-0000-0000-0000-000000000001', 'CUSTOMER', '20000000-0000-0000-0000-000000000001', '1.0', 'PASS', NULL,                              NOW() - INTERVAL '25 days'),
    ('cd000000-0000-0000-0000-000000000002', 'PEP',      '20000000-0000-0000-0000-000000000002', '1.0', 'EDD',  'Politically Exposed Person match', NOW() - INTERVAL '18 days'),
    ('cd000000-0000-0000-0000-000000000003', 'TRANSFER', 'a0000000-0000-0000-0000-000000000005', '1.0', 'PASS', NULL,                              NOW() - INTERVAL '3 days')
ON CONFLICT DO NOTHING;

-- ── Webhook endpoints + delivery history (Feature 26) ─────────────────────────
-- secret_encrypted is a placeholder; a real event delivery would fail to sign
-- (graceful) — seeded deliveries below are terminal so the dispatcher skips them.
INSERT INTO webhook_endpoints (id, owner_user_id, url, secret_encrypted, event_types, active, created_at, updated_at, version) VALUES
    ('eb000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'https://alice.example.com/hooks/bank', 'seed-placeholder', 'transfer.completed,kyc.status-changed', TRUE,  NOW() - INTERVAL '15 days', NOW(), 0),
    ('eb000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'https://bob.example.com/webhook',      'seed-placeholder', '*',                                     FALSE, NOW() - INTERVAL '8 days',  NOW(), 0)
ON CONFLICT DO NOTHING;

INSERT INTO webhook_deliveries (id, endpoint_id, event_type, payload, status, attempts, max_attempts, next_attempt_at, response_code, last_error, created_at, updated_at, version) VALUES
    ('db000000-0000-0000-0000-000000000001', 'eb000000-0000-0000-0000-000000000001', 'transfer.completed', '{"amount":"5000.0000","currency":"THB"}', 'DELIVERED', 1, 5, NOW() - INTERVAL '15 days', 200,  NULL,               NOW() - INTERVAL '15 days', NOW(), 0),
    ('db000000-0000-0000-0000-000000000002', 'eb000000-0000-0000-0000-000000000001', 'kyc.status-changed', '{"status":"VERIFIED"}',                   'DELIVERED', 1, 5, NOW() - INTERVAL '25 days', 200,  NULL,               NOW() - INTERVAL '25 days', NOW(), 0),
    ('db000000-0000-0000-0000-000000000003', 'eb000000-0000-0000-0000-000000000001', 'transfer.completed', '{"amount":"2000.0000","currency":"THB"}', 'FAILED',    5, 5, NOW() - INTERVAL '3 days',  500,  'HTTP 500 from endpoint', NOW() - INTERVAL '3 days',  NOW(), 0)
ON CONFLICT DO NOTHING;

-- ── Audit trail (Feature 7) ───────────────────────────────────────────────────
INSERT INTO audit_records (id, actor, action, before_json, after_json, trace_id, at) VALUES
    ('ad000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'auth:login-success', NULL, NULL,                          'trace-0001', NOW() - INTERVAL '2 hours'),
    ('ad000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000004', 'admin:freeze-account', NULL, '{"status":"FROZEN"}',        'trace-0002', NOW() - INTERVAL '1 days'),
    ('ad000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000004', 'fraud:approve',      NULL, '{"status":"APPROVED"}',        'trace-0003', NOW() - INTERVAL '6 days'),
    ('ad000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', 'customer:update',    '{"phone":"+66802223344"}', '{"phone":"+66809998877"}', 'trace-0004', NOW() - INTERVAL '4 days')
ON CONFLICT DO NOTHING;

-- ── Device / session records (Feature 16) ─────────────────────────────────────
INSERT INTO device_records (id, user_id, session_id, user_agent, ip_address, device_fingerprint, logged_in_at) VALUES
    ('de000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'aaaa1111-0000-0000-0000-000000000001', 'Mozilla/5.0 (iPhone)',  '203.0.113.10', 'fp-alice-iphone', NOW() - INTERVAL '2 hours'),
    ('de000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'aaaa1111-0000-0000-0000-000000000002', 'Mozilla/5.0 (Chrome)',  '203.0.113.11', 'fp-alice-laptop', NOW() - INTERVAL '3 days'),
    ('de000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'bbbb2222-0000-0000-0000-000000000001', 'Mozilla/5.0 (Android)', '198.51.100.7', 'fp-bob-android',  NOW() - INTERVAL '1 days')
ON CONFLICT DO NOTHING;

-- ── Notification log (Feature 13) ─────────────────────────────────────────────
INSERT INTO notification_log (id, event_id, channel, recipient, template, status, sent_at) VALUES
    ('0f000000-0000-0000-0000-000000000001', '0fe00000-0000-0000-0000-000000000001', 'EMAIL', 'customer', 'transfer_completed',  'SENT',   NOW() - INTERVAL '3 days'),
    ('0f000000-0000-0000-0000-000000000002', '0fe00000-0000-0000-0000-000000000002', 'EMAIL', 'customer', 'kyc_status_changed',   'SENT',   NOW() - INTERVAL '25 days'),
    ('0f000000-0000-0000-0000-000000000003', '0fe00000-0000-0000-0000-000000000003', 'EMAIL', 'customer', 'password_changed',     'FAILED', NOW() - INTERVAL '7 days')
ON CONFLICT DO NOTHING;

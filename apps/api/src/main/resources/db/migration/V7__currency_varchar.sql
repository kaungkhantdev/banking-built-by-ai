-- V7: align currency columns with the JPA mapping.
--
-- V1 (wallets) and V2 (ledger_entries) declared `currency` as CHAR(3), but the
-- entities map it to varchar(3) (@Column(length = 3)). With Hibernate's
-- ddl-auto: validate, the CHAR vs VARCHAR mismatch aborts boot against real
-- Postgres (H2 in tests is laxer, so it was never caught). VARCHAR(3) is also
-- the correct choice for ISO-4217 codes (no blank-padding surprises).
--
-- Idempotent: ALTER ... TYPE is a no-op if already VARCHAR.

ALTER TABLE wallets        ALTER COLUMN currency TYPE VARCHAR(3);
ALTER TABLE ledger_entries ALTER COLUMN currency TYPE VARCHAR(3);

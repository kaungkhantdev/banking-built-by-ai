-- V28: align all CHAR(3) currency columns added after V7 to VARCHAR(3)
-- so Hibernate ddl-auto:validate passes (it expects varchar, CHAR maps to bpchar).

ALTER TABLE exchange_rates  ALTER COLUMN from_currency TYPE VARCHAR(3);
ALTER TABLE exchange_rates  ALTER COLUMN to_currency   TYPE VARCHAR(3);

ALTER TABLE exchange_quotes ALTER COLUMN from_currency TYPE VARCHAR(3);
ALTER TABLE exchange_quotes ALTER COLUMN to_currency   TYPE VARCHAR(3);

ALTER TABLE limit_configs   ALTER COLUMN currency TYPE VARCHAR(3);
ALTER TABLE limit_usages    ALTER COLUMN currency TYPE VARCHAR(3);

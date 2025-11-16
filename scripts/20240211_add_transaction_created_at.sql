BEGIN;

ALTER TABLE transaction
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;

UPDATE transaction
SET created_at = date::timestamp AT TIME ZONE 'America/Santiago'
WHERE id > 0;

ALTER TABLE transaction
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN created_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transaction_customer_created_at
    ON transaction (customer_id, created_at);

COMMIT;

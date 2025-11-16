BEGIN;

ALTER TABLE transaction
    DROP COLUMN IF EXISTS backup_detail;

COMMIT;

BEGIN;

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS item_count INTEGER,
    ADD COLUMN IF NOT EXISTS backup_detail TEXT;

WITH parsed AS (
    SELECT id,
           detail AS original_detail,
           btrim(
                   regexp_replace(
                           regexp_replace(detail, '^\[Venta]:\s*', '', 'i'),
                           '\s*\[Prendas]:.*$', '',
                           'i'
                   )
           ) AS cleaned_detail,
           CASE
               WHEN detail ~ '\[Prendas]:\s*\d+' THEN
                   ((regexp_match(detail, '\[Prendas]:\s*(\d+)', 'i'))[1])::INTEGER
               ELSE NULL
           END AS parsed_count
    FROM transaction
    WHERE type = 'SALE'
)
UPDATE transaction t
SET backup_detail = COALESCE(t.backup_detail, parsed.original_detail),
    detail = parsed.cleaned_detail,
    item_count = COALESCE(parsed.parsed_count, t.item_count)
FROM parsed
WHERE t.id = parsed.id;

ALTER TABLE transaction
    DROP COLUMN IF EXISTS backup_detail;
    
COMMIT;

SELECT * FROM transaction WHERE customer_id = 315 ORDER BY id DESC;
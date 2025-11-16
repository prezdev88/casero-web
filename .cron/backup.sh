#!/usr/bin/env bash
set -euo pipefail

# Directorios de backup
BACKUPS_ROOT="$HOME/backups"
BACKUP_DIR="$BACKUPS_ROOT/casero"

PGUSER="casero"
PGPASSWORD="casero"
DBNAME="casero"

NOW=$(date +%F_%H-%M)

PG_DUMP="/usr/bin/pg_dump"
GZIP="/usr/bin/gzip"

# 0) Crear directorios si no existen
mkdir -p "$BACKUPS_ROOT"
mkdir -p "$BACKUP_DIR"

# 1) Eliminar respaldos anteriores (schema + data)
rm -f "$BACKUP_DIR/${DBNAME}_schema_"*.sql \
      "$BACKUP_DIR/${DBNAME}_"*.sql.gz || true

# 2) Backup del schema (solo estructura)
PGPASSWORD="$PGPASSWORD" "$PG_DUMP" \
  -h localhost \
  -p 5432 \
  -U "$PGUSER" \
  -d "$DBNAME" \
  -s > "$BACKUP_DIR/${DBNAME}_schema_${NOW}.sql"

# 3) Backup de la base completa (estructura + datos, comprimido)
PGPASSWORD="$PGPASSWORD" "$PG_DUMP" \
  -h localhost \
  -p 5432 \
  -U "$PGUSER" \
  "$DBNAME" \
  | "$GZIP" > "$BACKUP_DIR/${DBNAME}_${NOW}.sql.gz"
#!/usr/bin/env bash
set -euo pipefail

# Uso:
#   ./restore-casero.sh casero_2025-11-19_03-00.sql.gz qa
#   ./restore-casero.sh casero_2025-11-19_03-00.sql.gz prod

if [ "$#" -ne 2 ]; then
  echo "Uso: $0 <archivo.sql.gz> <qa|prod>"
  exit 1
fi

BACKUP_FILE="$1"
TARGET_ENV="$2"

# =========================
# Mapear entorno a puerto
# =========================
# PROD -> 5432
# QA   -> 5433

case "$TARGET_ENV" in
  qa)
    DB_PORT=5433
    ;;
  prod)
    DB_PORT=5432
    ;;
  *)
    echo "Entorno inválido: $TARGET_ENV. Usa 'qa' o 'prod'."
    exit 1
    ;;
esac

DB_HOST="localhost"
DB_NAME="casero"
DB_USER="casero"
DB_PASSWORD="casero"

echo "Restaurando backup '$BACKUP_FILE' en entorno '$TARGET_ENV' (puerto $DB_PORT)..."

# Validar que el archivo exista
if [ ! -f "$BACKUP_FILE" ]; then
  echo "Error: el archivo '$BACKUP_FILE' no existe."
  exit 1
fi

# =========================
# 1) Limpiar esquema public
# =========================

PGPASSWORD="$DB_PASSWORD" psql \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  "$DB_NAME" \
  -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO $DB_USER; GRANT ALL ON SCHEMA public TO public;"

# =========================
# 2) Restaurar backup
# =========================

gunzip -c "$BACKUP_FILE" \
  | PGPASSWORD="$DB_PASSWORD" psql \
      -h "$DB_HOST" \
      -p "$DB_PORT" \
      -U "$DB_USER" \
      "$DB_NAME"

echo "Restauración completada en entorno '$TARGET_ENV' (puerto $DB_PORT)."

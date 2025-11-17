#!/usr/bin/env bash
# crontab -e
# Todos los lunes a las 8am
# 0 8 * * 1 $HOME/mailgun.sh
set -euo pipefail

BACKUP_DIR="$HOME/backups/casero"
DBNAME="casero"

MAILGUN_DOMAIN="${MAILGUN_DOMAIN:-}"
MAILGUN_API_KEY="${MAILGUN_API_KEY:-}"
MAILGUN_FROM="${MAILGUN_FROM:-Backups <backups@mg.example.com>}"
MAILGUN_TO="${MAILGUN_TO:-tu-correo@example.com}"

NOW=$(date +%F_%H-%M)
SCHEMA_FILE=$(ls -t "$BACKUP_DIR/${DBNAME}_schema_"*.sql | head -n 1)
DATA_FILE=$(ls -t "$BACKUP_DIR/${DBNAME}_"*.sql.gz | head -n 1)

if [[ -z "$MAILGUN_DOMAIN" || -z "$MAILGUN_API_KEY" ]]; then
  echo "MAILGUN_DOMAIN y MAILGUN_API_KEY deben estar definidos" >&2
  exit 1
fi

if [[ -z "$SCHEMA_FILE" || -z "$DATA_FILE" ]]; then
  echo "No se encontraron archivos de backup en $BACKUP_DIR" >&2
  exit 1
fi

curl -s --user "api:$MAILGUN_API_KEY" \
  "https://api.mailgun.net/v3/$MAILGUN_DOMAIN/messages" \
  -F from="$MAILGUN_FROM" \
  -F to="$MAILGUN_TO" \
  -F subject="Backup Casero $NOW" \
  -F text="Adjunto el esquema y los datos del backup $NOW." \
  -F attachment=@"$SCHEMA_FILE" \
  -F attachment=@"$DATA_FILE"

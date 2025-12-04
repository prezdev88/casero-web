#!/usr/bin/env bash
set -euo pipefail

echo "Deteniendo contenedores QA..."
docker compose stop app-qa db-qa

echo "Eliminando contenedores QA..."
docker compose rm -f app-qa db-qa

echo "Eliminando volumen de base de datos QA..."
docker volume rm casero-web_postgres_data_qa || true

echo "Listo. Contenedores QA detenidos/eliminados y volumen de base de datos removido."

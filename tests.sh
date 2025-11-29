#!/usr/bin/env bash
set -euo pipefail

# Runner local para la suite E2E.
# Variables opcionales:
#   BASE_URL (default: http://localhost:8080/casero)
#   ADMIN_PIN (default: 1111)
#   E2E_START_COMMAND (default: vacío, porque usamos docker compose para la app)
#   PLAYWRIGHT_HEADLESS (default: false; pon true para headless)
#   E2E_SPEC (default: vacío; si se setea, ejecuta solo ese spec. Ej: e2e/refund.spec.js)
#   --menu (flag opcional): muestra menú interactivo para elegir entre todos los specs o uno en particular

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"
COMPOSE_FILE="$ROOT_DIR/docker-compose.yml"
COMPOSE_E2E_FILE="$ROOT_DIR/docker-compose.e2e.yml"
MENU_MODE=false
SPEC_FILTER="${E2E_SPEC:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --menu)
      MENU_MODE=true
      ;;
    --spec)
      SPEC_FILTER="${2:-}"
      shift
      ;;
    *)
      echo "Uso: $0 [--menu] [--spec ruta/al/spec]" >&2
      exit 1
      ;;
  esac
  shift
done

choose_spec() {
  mapfile -t specs < <(ls e2e/*.spec.js 2>/dev/null || true)
  if [[ ${#specs[@]} -eq 0 ]]; then
    echo "No se encontraron specs en e2e/*.spec.js" >&2
    exit 1
  fi
  echo "Selecciona qué correr:"
  echo "0) Todos los specs"
  for i in "${!specs[@]}"; do
    echo "$((i + 1))) ${specs[$i]}"
  done
  read -r -p "Opción: " choice
  if [[ "$choice" == "0" ]]; then
    SPEC_FILTER=""
    return
  fi
  local idx=$((choice - 1))
  if [[ $idx -lt 0 || $idx -ge ${#specs[@]} ]]; then
    echo "Opción inválida" >&2
    exit 1
  fi
  SPEC_FILTER="${specs[$idx]}"
}

if [[ "$MENU_MODE" == true && -z "$SPEC_FILTER" ]]; then
  choose_spec
fi

cleanup() {
  echo ">> Stopping docker compose services..."
  docker compose -f "$COMPOSE_FILE" -f "$COMPOSE_E2E_FILE" down>/dev/null 2>&1 || true
}
trap cleanup EXIT

BASE_URL="${BASE_URL:-http://localhost:8080/casero}"
ADMIN_PIN="${ADMIN_PIN:-1111}"
# Usamos la app en Docker Compose por defecto; deja vacío para reutilizarla.
E2E_START_COMMAND="${E2E_START_COMMAND:-}"
PLAYWRIGHT_HEADLESS="${PLAYWRIGHT_HEADLESS:-false}"

# Arch no está soportado oficialmente; saltamos validación de host.
export PLAYWRIGHT_SKIP_VALIDATE_HOST_REQUIREMENTS=1

echo ">> Ensuring JS dependencies..."
npm install

echo ">> Installing Playwright browsers (may show warnings on Arch)..."
npx playwright install

echo ">> Ensuring proxy-net docker network exists..."
if ! docker network inspect proxy-net >/dev/null 2>&1; then
  docker network create proxy-net >/dev/null
fi

echo ">> Starting app and Postgres via docker compose..."
docker compose -f "$COMPOSE_FILE" -f "$COMPOSE_E2E_FILE" up -d --build db app

echo ">> Waiting for app to be ready at ${BASE_URL}..."
ready=false
for i in {1..60}; do
  if curl -fsS "${BASE_URL}/login" >/dev/null 2>&1; then
    ready=true
    break
  fi
  sleep 2
done

if [[ "$ready" != true ]]; then
  echo "App no respondió en ${BASE_URL} tras esperar." >&2
  exit 1
fi

echo ">> Ensuring seed admin/normal users in DB..."
docker compose -f "$COMPOSE_FILE" -f "$COMPOSE_E2E_FILE" exec -T db psql -U casero -d casero <<'SQL'
INSERT INTO app_user (name, role, pin_hash, pin_salt, pin_fingerprint, enabled)
VALUES
  ('Administrador', 'ADMIN', '39f3a563f18cc784297b820fd271db2f0e3e1da6330333e3afa8d1dda95d9cc7', 'a1b2c3d4e5f60708', '0ffe1abd1a08215353c233d6e009613e95eec4253832a761af28ff37ac5a150c', true),
  ('Vendedor', 'NORMAL', '154b36cf116dc0c09ce4f7727916a396e880b3d27a154cecc9373f668fbd21b5', '0f1e2d3c4b5a6978', 'edee29f882543b956620b26d0ee0e7e950399b1c4222f5de05e06425b4c995e9', true)
ON CONFLICT (pin_fingerprint) DO UPDATE
SET name = EXCLUDED.name, role = EXCLUDED.role, enabled = true;
SQL

echo ">> Running E2E suite"
echo "   BASE_URL=${BASE_URL}"
echo "   ADMIN_PIN=${ADMIN_PIN}"
echo "   PLAYWRIGHT_HEADLESS=${PLAYWRIGHT_HEADLESS}"
echo "   PLAYWRIGHT_WORKERS=${PLAYWRIGHT_WORKERS:-1}"
if [[ -n "$SPEC_FILTER" ]]; then
  echo "   E2E_SPEC=${SPEC_FILTER}"
fi
if [[ -n "$E2E_START_COMMAND" ]]; then
  echo "   E2E_START_COMMAND=${E2E_START_COMMAND}"
else
  echo "   Reusing existing app server (no start command set)"
fi

TEST_CMD=(npm test)
if [[ -n "$SPEC_FILTER" ]]; then
  TEST_CMD+=(-- "$SPEC_FILTER")
fi

BASE_URL="$BASE_URL" \
ADMIN_PIN="$ADMIN_PIN" \
E2E_START_COMMAND="$E2E_START_COMMAND" \
PLAYWRIGHT_HEADLESS="$PLAYWRIGHT_HEADLESS" \
PLAYWRIGHT_WORKERS="${PLAYWRIGHT_WORKERS:-1}" \
"${TEST_CMD[@]}"

#!/usr/bin/env bash
set -euo pipefail

# Ejecuta la suite E2E dentro de contenedores.
# Uso:
#   ./run-tests-docker.sh <branch>
# Variables opcionales:
#   BRANCH (si no pasas argumento) -> rama a clonar antes de correr tests
#   REPO_URL (default: https://github.com/prezdev88/casero-web.git)
#   BASE_URL (default: http://app:8080) -> URL interna hacia el servicio app en el compose
#   ADMIN_PIN (default: 1111)
#   PLAYWRIGHT_HEADLESS (default: true)

command -v docker >/dev/null 2>&1 || { echo "docker no encontrado" >&2; exit 1; }
command -v git >/dev/null 2>&1 || { echo "git no encontrado" >&2; exit 1; }

BRANCH="${1:-${BRANCH:-develop}}"
REPO_URL="${REPO_URL:-https://github.com/prezdev88/casero-web.git}"
BASE_URL="${BASE_URL:-http://app:8080}"
ADMIN_PIN="${ADMIN_PIN:-1111}"
PLAYWRIGHT_HEADLESS="${PLAYWRIGHT_HEADLESS:-true}"

WORKDIR="$(mktemp -d)"
CLONE_DIR="$WORKDIR/repo"
COMPOSE_FILE="$WORKDIR/docker-compose.e2e.yml"
PROJECT_NAME="casero-e2e-${BRANCH//\//-}"

echo ">> Clonando rama $BRANCH desde $REPO_URL ..."
git clone --branch "$BRANCH" --single-branch --depth 1 "$REPO_URL" "$CLONE_DIR"

cat > "$COMPOSE_FILE" <<EOF
services:
  db:
    image: postgres:15
    environment:
      POSTGRES_DB: casero
      POSTGRES_USER: casero
      POSTGRES_PASSWORD: casero
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U casero"]
      interval: 10s
      timeout: 5s
      retries: 10

  app:
    build:
      context: $CLONE_DIR
      dockerfile: Dockerfile
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/casero
      SPRING_DATASOURCE_USERNAME: casero
      SPRING_DATASOURCE_PASSWORD: casero
      SPRING_PROFILES_ACTIVE: local
    expose:
      - "8080"

  e2e:
    image: mcr.microsoft.com/playwright:v1.57.0-jammy
    working_dir: /workspace
    volumes:
      - $CLONE_DIR:/workspace
    depends_on:
      app:
        condition: service_started
    environment:
      BASE_URL: $BASE_URL
      ADMIN_PIN: $ADMIN_PIN
      E2E_START_COMMAND: ""
      PLAYWRIGHT_HEADLESS: $PLAYWRIGHT_HEADLESS
    command: >
      bash -lc "
        apt-get update &&
        apt-get install -y curl &&
        npm ci &&
        until curl -fsSL $BASE_URL/login >/dev/null; do echo 'esperando app...'; sleep 2; done;
        npm test
      "

volumes:
  db-data:
    driver: local
EOF

echo ">> Levantando stack E2E con proyecto $PROJECT_NAME ..."
COMPOSE_PROJECT_NAME="$PROJECT_NAME" docker compose -f "$COMPOSE_FILE" up --build --abort-on-container-exit e2e

echo ">> Bajando stack y limpiando..."
COMPOSE_PROJECT_NAME="$PROJECT_NAME" docker compose -f "$COMPOSE_FILE" down -v
rm -rf "$WORKDIR"

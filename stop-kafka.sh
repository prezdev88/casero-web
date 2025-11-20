#!/usr/bin/env bash
set -euo pipefail

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command not found" >&2
  exit 1
fi

containers=(
  "${KAFKA_UI_CONTAINER:-casero-kafka-ui}"
  "${KAFKA_CONTAINER:-casero-kafka}"
  "${ZOOKEEPER_CONTAINER:-casero-zookeeper}"
)

stopped_any=false

for container in "${containers[@]}"; do
  if docker ps --filter "name=^/${container}$" --format '{{.Names}}' | grep -q .; then
    docker stop "$container" >/dev/null
    echo "Stopped $container"
    stopped_any=true
  else
    echo "$container is not running"
  fi
done

if [ "$stopped_any" = false ]; then
  echo "No Kafka-related containers were running."
fi

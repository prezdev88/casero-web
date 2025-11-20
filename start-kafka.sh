#!/usr/bin/env bash
set -euo pipefail

# Basic docker-compose alternative to bring up Kafka + Kafka UI locally.

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command not found" >&2
  exit 1
fi

NETWORK="${KAFKA_DOCKER_NETWORK:-casero-web_internal}"
ZOOKEEPER_CONTAINER="${ZOOKEEPER_CONTAINER:-casero-zookeeper}"
KAFKA_CONTAINER="${KAFKA_CONTAINER:-casero-kafka}"
KAFKA_UI_CONTAINER="${KAFKA_UI_CONTAINER:-casero-kafka-ui}"
HOST_ZOOKEEPER_PORT="${HOST_ZOOKEEPER_PORT:-2181}"
HOST_KAFKA_PORT="${HOST_KAFKA_PORT:-9092}"
HOST_KAFKA_UI_PORT="${HOST_KAFKA_UI_PORT:-8082}"

container_exists() {
  docker ps -a --filter "name=^/${1}$" --format '{{.Names}}' | grep -q .
}

container_running() {
  docker ps --filter "name=^/${1}$" --format '{{.Names}}' | grep -q .
}

ensure_network() {
  if ! docker network ls --format '{{.Name}}' | grep -Fx "$NETWORK" >/dev/null; then
    docker network create "$NETWORK" >/dev/null
  fi
}

ensure_zookeeper() {
  if container_running "$ZOOKEEPER_CONTAINER"; then
    echo "$ZOOKEEPER_CONTAINER already running"
  elif container_exists "$ZOOKEEPER_CONTAINER"; then
    docker start "$ZOOKEEPER_CONTAINER" >/dev/null
  else
    docker run -d \
      --name "$ZOOKEEPER_CONTAINER" \
      --network "$NETWORK" \
      -p "${HOST_ZOOKEEPER_PORT}:2181" \
      -e ZOOKEEPER_CLIENT_PORT=2181 \
      -e ZOOKEEPER_TICK_TIME=2000 \
      confluentinc/cp-zookeeper:7.5.0 >/dev/null
  fi
}

ensure_kafka() {
  if container_running "$KAFKA_CONTAINER"; then
    echo "$KAFKA_CONTAINER already running"
  elif container_exists "$KAFKA_CONTAINER"; then
    docker start "$KAFKA_CONTAINER" >/dev/null
  else
    docker run -d \
      --name "$KAFKA_CONTAINER" \
      --network "$NETWORK" \
      -p "${HOST_KAFKA_PORT}:9092" \
      -e KAFKA_BROKER_ID=1 \
      -e KAFKA_ZOOKEEPER_CONNECT="${ZOOKEEPER_CONTAINER}:2181" \
      -e KAFKA_ADVERTISED_LISTENERS="PLAINTEXT://${KAFKA_CONTAINER}:9092" \
      -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP="PLAINTEXT:PLAINTEXT" \
      -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
      -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 \
      confluentinc/cp-kafka:7.5.0 >/dev/null
  fi
}

ensure_kafka_ui() {
  if container_running "$KAFKA_UI_CONTAINER"; then
    echo "$KAFKA_UI_CONTAINER already running"
  elif container_exists "$KAFKA_UI_CONTAINER"; then
    docker start "$KAFKA_UI_CONTAINER" >/dev/null
  else
    docker run -d \
      --name "$KAFKA_UI_CONTAINER" \
      --network "$NETWORK" \
      -p "${HOST_KAFKA_UI_PORT}:8080" \
      -e KAFKA_CLUSTERS_0_NAME=casero-local \
      -e KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS="${KAFKA_CONTAINER}:9092" \
      -e KAFKA_CLUSTERS_0_ZOOKEEPER="${ZOOKEEPER_CONTAINER}:2181" \
      provectuslabs/kafka-ui:latest >/dev/null
  fi
}

main() {
  ensure_network
  ensure_zookeeper
  ensure_kafka
  ensure_kafka_ui
  echo "Kafka y Kafka UI están en marcha."
  echo "Kafka: localhost:${HOST_KAFKA_PORT}, UI: http://localhost:${HOST_KAFKA_UI_PORT}"
}

main "$@"

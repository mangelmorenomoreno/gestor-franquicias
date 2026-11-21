#!/usr/bin/env bash
set -Eeuo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

# El archivo .env es opcional y permite sobrescribir las credenciales locales.
if [[ -f .env ]]; then
  set -a
  # shellcheck source=/dev/null
  source ./.env
  set +a
fi

export DB_USERNAME="${DB_USERNAME:-gestor_franquicias}"
export DB_PASSWORD="${DB_PASSWORD:-franquicias_demo_local_2026}"
export DB_URL="r2dbc:postgresql://localhost:5434/gestor_franquicias"
export DB_MIGRATION_URL="jdbc:postgresql://localhost:5434/gestor_franquicias"

# Solo queda PostgreSQL en Docker; Spring Boot se ejecuta con Maven.
docker compose stop api
docker compose up -d base-datos

exec mvn spring-boot:run

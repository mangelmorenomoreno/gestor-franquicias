#!/usr/bin/env bash
set -Eeuo pipefail

perfil="${PERFIL:-dev}"
case "$perfil" in dev|test|stage|prod) ;; *) echo "Perfil no permitido" >&2; exit 1 ;; esac

identificador="franquicias-ci-${GITHUB_RUN_ID:-local}-$$"
red="red-${identificador}"
base_datos="postgres-${identificador}"
aplicacion="api-${identificador}"
clave_base_datos="$(openssl rand -hex 20)"
puerto_api=18088

limpiar() {
    docker rm -f "$aplicacion" "$base_datos" >/dev/null 2>&1 || true
    docker network rm "$red" >/dev/null 2>&1 || true
}
trap limpiar EXIT
docker network create "$red" >/dev/null
docker run -d --name "$base_datos" --network "$red" --network-alias base-datos \
    -e POSTGRES_DB=gestor_franquicias -e POSTGRES_USER=gestor_franquicias \
    -e "POSTGRES_PASSWORD=$clave_base_datos" postgres:16-alpine >/dev/null

echo "Comprobando PostgreSQL..."
base_disponible=false
for intento in $(seq 1 60); do
    if docker exec "$base_datos" pg_isready -U gestor_franquicias -d gestor_franquicias >/dev/null 2>&1; then
        base_disponible=true
        break
    fi
    sleep 2
done
if [ "$base_disponible" != true ]; then
    docker logs "$base_datos" >&2
    exit 1
fi

docker run -d --name "$aplicacion" --network "$red" \
    --publish "127.0.0.1:$puerto_api:8080" \
    --read-only --tmpfs /tmp:rw,nosuid,size=64m --cap-drop ALL \
    --security-opt no-new-privileges:true \
    -e "SPRING_PROFILES_ACTIVE=$perfil" \
    -e DB_URL=r2dbc:postgresql://base-datos:5432/gestor_franquicias \
    -e DB_MIGRATION_URL=jdbc:postgresql://base-datos:5432/gestor_franquicias \
    -e DB_USERNAME=gestor_franquicias -e "DB_PASSWORD=$clave_base_datos" \
    gestor-franquicias:validacion >/dev/null

verificar_api() {
    local disponible=false
    for intento in $(seq 1 90); do
        if curl -fsS --max-time 2 "http://127.0.0.1:$puerto_api/actuator/health" 2>/dev/null \
            | python3 -c 'import json,sys; assert json.load(sys.stdin)["status"] == "UP"' >/dev/null 2>&1; then
            disponible=true
            break
        fi
        if [ "$(docker inspect -f '{{.State.Running}}' "$aplicacion")" != true ]; then
            break
        fi
        sleep 2
    done
    if [ "$disponible" != true ]; then
        docker logs "$aplicacion" >&2
        exit 1
    fi
}
echo "Comprobando conexión R2DBC y migración Flyway para el perfil $perfil..."
verificar_api

respuesta="$(curl -fsS --max-time 10 -X POST \
    "http://127.0.0.1:$puerto_api/api/v1/franquicias" \
    -H 'Content-Type: application/json' \
    -d '{"nombre":"Validacion de contenedor"}')"
id_franquicia="$(printf '%s' "$respuesta" | python3 -c \
    'import json,sys; r=json.load(sys.stdin); assert r["success"]; print(r["data"]["idFranquicia"])')"
curl -fsS --max-time 10 "http://127.0.0.1:$puerto_api/api/v1/franquicias/$id_franquicia" \
    | python3 -c 'import json,sys; r=json.load(sys.stdin); assert r["success"]; assert r["data"]["sucursales"] == []'

echo "Reiniciando la API para comprobar persistencia en PostgreSQL..."
docker restart "$aplicacion" >/dev/null
verificar_api
curl -fsS --max-time 10 "http://127.0.0.1:$puerto_api/api/v1/franquicias/$id_franquicia" \
    | python3 -c 'import json,sys; r=json.load(sys.stdin); assert r["success"]; assert r["data"]["nombre"] == "Validacion de contenedor"'
echo "Conexión, migraciones y persistencia verificadas con perfil $perfil."

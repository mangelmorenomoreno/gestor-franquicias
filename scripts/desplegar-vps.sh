#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

ambiente="${1:?Indicar ambiente dev, test, stage o prod}"
case "$ambiente" in
  dev) puerto_esperado=8086 ;;
  test) puerto_esperado=8087 ;;
  stage) puerto_esperado=8089 ;;
  prod) puerto_esperado=8088 ;;
  *) echo "Ambiente no autorizado: $ambiente" >&2; exit 1 ;;
esac

directorio="/opt/gestor-franquicias/$ambiente"
compose="docker-compose.$ambiente.yml"
postgres="gestor-franquicias-$ambiente-postgres"
red="gestor-franquicias-$ambiente-red"

if [[ "$(id -u)" -ne 0 ]]; then
  echo "Se requieren privilegios administrativos en el VPS." >&2
  exit 1
fi
cd "$directorio"
# Credenciales de GHCR efímeras, aisladas de las apps del VPS que usan Docker.
export DOCKER_CONFIG=/run/gestor-franquicias-ghcr
test -f .env
test -f "$compose"
test -f infra/terraform/main.tf
set -a
# shellcheck source=/dev/null
source ./.env
set +a

if [[ ! "${DB_USERNAME:-}" =~ ^[a-z_][a-z0-9_]*$ ]] ||
   [[ ! "${DB_PASSWORD:-}" =~ ^[A-Za-z0-9_-]{20,}$ ]] ||
   [[ ! "${IMAGEN_BACKEND:-}" =~ ^ghcr[.]io/mangelmorenomoreno/gestor-franquicias:[0-9a-f]{40}$ ]] ||
   [[ "${IP_ESCUCHA_API:-}" != "127.0.0.1" ]] ||
   [[ "${PUERTO_API:-}" != "$puerto_esperado" ]]; then
  echo "Configuración o puerto no válidos para $ambiente." >&2
  exit 1
fi

if ! command -v terraform >/dev/null 2>&1; then
  echo "Instalando Terraform desde el repositorio oficial..."
  export DEBIAN_FRONTEND=noninteractive
  apt-get update
  apt-get install -y --no-install-recommends curl ca-certificates gnupg
  keyring="/usr/share/keyrings/hashicorp-archive-keyring.gpg"
  curl -fsSL https://apt.releases.hashicorp.com/gpg \
    | gpg --batch --yes --dearmor -o "$keyring"
  # apt ejecuta gpgv sin privilegios y debe poder leer el keyring público.
  # El script mantiene umask 077 para proteger secretos en disco.
  chmod 0644 "$keyring"
  fingerprint="$(gpg --no-default-keyring --keyring "$keyring" \
    --with-colons --fingerprint | awk -F: '$1 == "fpr" {print $10; exit}')"
  if [[ "$fingerprint" != "D55C0D1AC78A8D8126CB631CFC9CA96ACA026560" ]]; then
    echo "La huella de la clave de HashiCorp no coincide." >&2
    exit 1
  fi
  # shellcheck source=/dev/null
  source /etc/os-release
  if [[ "$ID" != ubuntu || "$VERSION_CODENAME" != resolute ]]; then
    echo "Se espera Ubuntu 26.04 (resolute)." >&2
    exit 1
  fi
  printf 'deb [arch=%s signed-by=%s] https://apt.releases.hashicorp.com %s main\n' \
    "$(dpkg --print-architecture)" "$keyring" "$VERSION_CODENAME" \
    > /etc/apt/sources.list.d/hashicorp.list
  apt-get update
  apt-get install -y --no-install-recommends terraform
fi

terraform version | head -n 1
docker version --format '{{.Server.Version}}' >/dev/null
docker compose version >/dev/null
command -v curl >/dev/null

# Los estados Terraform están aislados por directorio de ambiente.
export TF_VAR_ambiente="$ambiente"
export TF_VAR_usuario_base_datos="$DB_USERNAME"
export TF_VAR_clave_base_datos="$DB_PASSWORD"
chmod 700 "$directorio/infra/terraform"
cd "$directorio/infra/terraform"

terraform init -input=false -no-color
terraform validate -no-color

plan="$(mktemp "$PWD/plan.XXXXXX.tfplan")"
trap 'rm -f "$plan"' EXIT
terraform plan -input=false -no-color -out="$plan" >/dev/null

# La configuración no puede afectar recursos de otro ambiente.
terraform show -json "$plan" | python3 -c '
import json,sys
datos=json.load(sys.stdin)
permitidos={
 "docker_network.franquicias",
 "docker_volume.datos_franquicias",
 "docker_image.postgres",
 "docker_container.postgres",
}
for cambio in datos.get("resource_changes", []):
    direccion=cambio["address"]
    acciones=cambio["change"]["actions"]
    if direccion not in permitidos:
        raise SystemExit("Recurso no autorizado: "+direccion)
    if direccion == "docker_volume.datos_franquicias" and "delete" in acciones:
        raise SystemExit("No se puede destruir el volumen PostgreSQL.")
print("Plan Terraform del ambiente validado.")
'

cd "$directorio"
echo "Verificando imagen de $ambiente en GHCR..."
docker pull "$IMAGEN_BACKEND" >/dev/null
echo "Aplicando infraestructura PostgreSQL de $ambiente..."
terraform -chdir=infra/terraform apply -input=false -no-color "$plan"

docker network inspect "$red" >/dev/null
postgres_disponible=false
for intento in $(seq 1 60); do
  estado="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}sin-healthcheck{{end}}' \
    "$postgres" 2>/dev/null || true)"
  if [[ "$estado" == healthy ]]; then
    postgres_disponible=true
    break
  fi
  sleep 2
done
if [[ "$postgres_disponible" != true ]]; then
  docker logs --tail 60 "$postgres" >&2 || true
  echo "PostgreSQL no está saludable en $ambiente." >&2
  exit 1
fi

docker compose -p "gestor-franquicias-$ambiente" --env-file .env -f "$compose" config -q
docker compose -p "gestor-franquicias-$ambiente" --env-file .env -f "$compose" \
  up -d --no-deps api

api_disponible=false
for intento in $(seq 1 60); do
  if curl -fsS --max-time 3 "http://127.0.0.1:$PUERTO_API/actuator/health" 2>/dev/null \
    | python3 -c 'import json,sys; assert json.load(sys.stdin)["status"]=="UP"' \
      >/dev/null 2>&1; then
    api_disponible=true
    break
  fi
  sleep 2
done
if [[ "$api_disponible" != true ]]; then
  docker compose -p "gestor-franquicias-$ambiente" --env-file .env -f "$compose" \
    logs --tail=90 api >&2 || true
  echo "La API no respondió correctamente en $ambiente." >&2
  exit 1
fi

# Validar que la API está unida exclusivamente a la red esperada.
contenedor_api="$(docker compose -p "gestor-franquicias-$ambiente" -f "$compose" ps -q api)"
redes_api="$(docker inspect -f '{{range $nombre, $_ := .NetworkSettings.Networks}}{{println $nombre}}{{end}}' "$contenedor_api")"
if [[ "$redes_api" != "$red" ]]; then
  echo "La API está conectada a una red inesperada: $redes_api" >&2
  exit 1
fi
echo "Ambiente $ambiente validado en 127.0.0.1:$PUERTO_API"
docker compose -p "gestor-franquicias-$ambiente" --env-file .env -f "$compose" ps

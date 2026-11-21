#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

directorio="/opt/gestor-franquicias"
cd "$directorio"

if [[ "$(id -u)" -ne 0 ]]; then
    echo "Este procedimiento requiere privilegios administrativos en el VPS." >&2
    exit 1
fi

if [[ ! -f .env || ! -f docker-compose.prod.yml ]]; then
    echo "Faltan los archivos de configuración del proyecto." >&2
    exit 1
fi

# El archivo .env lo construye el workflow a partir de los secretos de GitHub.
set -a
# shellcheck source=/dev/null
source ./.env
set +a

if [[ ! "$DB_USERNAME" =~ ^[a-z_][a-z0-9_]*$ ]] ||
   [[ ! "$DB_PASSWORD" =~ ^[A-Za-z0-9_-]{20,}$ ]] ||
   [[ ! "$IMAGEN_BACKEND" =~ ^ghcr[.]io/mangelmorenomoreno/gestor-franquicias:[0-9a-f]{40}$ ]] ||
   [[ "$IP_ESCUCHA_API" != "127.0.0.1" && "$IP_ESCUCHA_API" != "0.0.0.0" ]] ||
   [[ ! "$PUERTO_API" =~ ^[0-9]{4,5}$ ]]; then
    echo "La configuración del despliegue no supera las validaciones." >&2
    exit 1
fi

if ! command -v terraform >/dev/null 2>&1; then
    echo "Instalando Terraform desde el repositorio oficial de HashiCorp..."
    export DEBIAN_FRONTEND=noninteractive
    apt-get update
    apt-get install -y --no-install-recommends curl ca-certificates gnupg

    keyring="/usr/share/keyrings/hashicorp-archive-keyring.gpg"
    curl -fsSL https://apt.releases.hashicorp.com/gpg \
        | gpg --batch --yes --dearmor -o "$keyring"

    fingerprint="$(gpg --no-default-keyring --keyring "$keyring" \
        --with-colons --fingerprint | awk -F: '$1 == "fpr" {print $10; exit}')"
    if [[ "$fingerprint" != "D55C0D1AC78A8D8126CB631CFC9CA96ACA026560" ]]; then
        echo "La firma de HashiCorp no coincide con la huella esperada." >&2
        exit 1
    fi

    # Ubuntu 26.04.1 utiliza VERSION_CODENAME=resolute.
    # shellcheck source=/dev/null
    source /etc/os-release
    if [[ "$ID" != ubuntu || "$VERSION_CODENAME" != resolute ]]; then
        echo "Este instalador inicial está previsto para Ubuntu 26.04 (resolute)." >&2
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

# El estado de Terraform se conserva en el VPS, sin copiarse a GitHub Actions.
# El volumen PostgreSQL no se debe eliminar por un cambio de despliegue.
export TF_VAR_usuario_base_datos="$DB_USERNAME"
export TF_VAR_clave_base_datos="$DB_PASSWORD"
chmod 700 "$directorio/infra/terraform"
cd "$directorio/infra/terraform"

terraform init -input=false -no-color
terraform validate -no-color

plan="$(mktemp "$PWD/plan.XXXXXX.tfplan")"
limpiar_plan() {
    rm -f "$plan"
}
trap limpiar_plan EXIT

echo "Preparando plan Terraform para los recursos exclusivos del proyecto..."
terraform plan -input=false -no-color -out="$plan" >/dev/null

# Rechazar operaciones sobre recursos ajenos y destrucción del volumen persistente.
terraform show -json "$plan" | python3 -c '
import json, sys
datos = json.load(sys.stdin)
permitidos = {
    "docker_network.franquicias",
    "docker_volume.datos_franquicias",
    "docker_image.postgres",
    "docker_container.postgres",
}
for cambio in datos.get("resource_changes", []):
    direccion = cambio["address"]
    acciones = cambio["change"]["actions"]
    if direccion not in permitidos:
        raise SystemExit(f"El plan incluye un recurso no autorizado: {direccion}")
    if direccion == "docker_volume.datos_franquicias" and "delete" in acciones:
        raise SystemExit("El plan intenta eliminar el volumen persistente de PostgreSQL.")
print("Plan revisado: recursos autorizados y volumen protegido.")
'

# Validar acceso a la imagen antes de aprovisionar contenedores.
cd "$directorio"
echo "Verificando imagen Docker publicada..."
docker pull "$IMAGEN_BACKEND" >/dev/null

echo "Aplicando infraestructura de red, volumen y PostgreSQL..."
terraform -chdir=infra/terraform apply -input=false -no-color "$plan"

echo "Comprobando disponibilidad de PostgreSQL..."
postgres_disponible=false
for intento in $(seq 1 60); do
    estado="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}sin-healthcheck{{end}}' \
        gestor-franquicias-postgres 2>/dev/null || true)"
    if [[ "$estado" == healthy ]]; then
        postgres_disponible=true
        break
    fi
    sleep 2
done
if [[ "$postgres_disponible" != true ]]; then
    echo "PostgreSQL no alcanzó el estado healthy." >&2
    docker logs --tail 70 gestor-franquicias-postgres >&2 || true
    exit 1
fi

echo "Actualizando únicamente la API del proyecto..."
docker compose -p gestor-franquicias --env-file .env -f docker-compose.prod.yml config -q
docker compose -p gestor-franquicias --env-file .env -f docker-compose.prod.yml \
    up -d --no-deps api

echo "Verificando respuesta de salud local..."
servicio_disponible=false
for intento in $(seq 1 60); do
    if curl -fsS --max-time 3 "http://127.0.0.1:$PUERTO_API/actuator/health" 2>/dev/null \
        | python3 -c 'import json,sys; assert json.load(sys.stdin)["status"] == "UP"' \
            >/dev/null 2>&1; then
        servicio_disponible=true
        break
    fi
    sleep 2
done

if [[ "$servicio_disponible" != true ]]; then
    docker compose -p gestor-franquicias --env-file .env -f docker-compose.prod.yml \
        logs --tail=90 api >&2 || true
    echo "La API no respondió correctamente después del despliegue." >&2
    exit 1
fi

echo "API desplegada y validada en el puerto $PUERTO_API del VPS."
docker compose -p gestor-franquicias --env-file .env -f docker-compose.prod.yml ps

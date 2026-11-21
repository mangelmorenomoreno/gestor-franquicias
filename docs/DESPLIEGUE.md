# Despliegue automatizado en el VPS

La aplicación dispone de un workflow manual de GitHub Actions que automatiza la preparación del servidor y la puesta en marcha de la API. Una vez activado desde la rama `main`, la ejecución transfiere los archivos, instala Terraform si hace falta, inicializa y aplica la infraestructura exclusiva de este proyecto, descarga la imagen versionada desde GHCR, inicia el contenedor y comprueba el endpoint de salud.

El despliegue **no se ha ejecutado todavía** en el VPS. La automatización solo se utilizará cuando el propietario haya configurado el acceso SSH y los secretos correspondientes.

## Requisitos del servidor

- Ubuntu 26.04.1 LTS en arquitectura amd64.
- Docker Engine y Docker Compose instalados y funcionales.
- Acceso saliente a `apt.releases.hashicorp.com`, `registry.terraform.io`, Docker Hub y `ghcr.io`.
- Acceso SSH de un usuario con permisos de `root` o `sudo -n` para instalar Terraform y administrar los recursos Docker.
- Puerto local 8088 disponible.

Terraform se instala automáticamente desde el repositorio oficial de HashiCorp únicamente cuando no está instalado, verificando la huella del certificado de firma del repositorio. No se modifican contenedores, redes, volúmenes o directorios ajenos a Gestor de Franquicias.

## Configuración inicial en GitHub

Abrir `Settings → Environments` y crear el entorno `produccion`. Configurar las siguientes entradas como **secrets** del entorno:

| Secreto | Contenido |
|---|---|
| `VPS_HOST` | IP o nombre DNS del VPS |
| `VPS_USER` | Usuario para la conexión SSH |
| `VPS_SSH_KEY` | Clave privada SSH con autorización en el VPS |
| `VPS_KNOWN_HOSTS` | Clave pública del host SSH verificada previamente |
| `DB_PASSWORD` | Contraseña aleatoria de PostgreSQL, mínimo 20 caracteres; letras, números, guion o guion bajo |

Para generar una contraseña segura en una terminal local:

~~~bash
openssl rand -hex 32
~~~

No pegar la contraseña ni la clave privada SSH en el repositorio, en un issue ni en un chat. `VPS_KNOWN_HOSTS` debe comprobarse contra la huella auténtica del servidor antes de introducirla en GitHub.

Si la imagen de GHCR es privada, configurar además `GHCR_USUARIO` y `GHCR_LEER_PAQUETE` (token con permiso `read:packages`). El workflow utiliza estas credenciales únicamente cuando están disponibles. Si la imagen es pública, no hacen falta.

Se recomienda activar la aprobación del entorno `produccion` si el plan de GitHub y la configuración del repositorio lo permiten.

## Primera ejecución

1. Confirmar que el workflow `Validación backend` terminó correctamente en `main`.
2. Confirmar que el workflow `Publicar imagen Docker` publicó la etiqueta correspondiente al último SHA de `main`.
3. Abrir `Actions → Desplegar API en el VPS → Run workflow`, con la rama `main`.
4. Aprobar la ejecución cuando el entorno lo solicite y revisar el resultado final.

La instalación inicial ya no requiere escribir los comandos `apt`, `terraform init`, `terraform plan` o `terraform apply` manualmente en la consola del VPS. El workflow los ejecuta en orden bajo la autorización del usuario.

## Recursos que administra Terraform

- Red Docker: `gestor-franquicias-red`.
- Volumen de PostgreSQL: `gestor-franquicias-datos`.
- Contenedor PostgreSQL: `gestor-franquicias-postgres`.
- Imagen de PostgreSQL 16.

Docker Compose administra únicamente el servicio `api` de este proyecto; el puerto está vinculado a `127.0.0.1:8088` por seguridad y el contenedor de PostgreSQL no publica un puerto al exterior.

La configuración y el estado de Terraform se conservan en `/opt/gestor-franquicias`. El estado puede incluir valores sensibles y debe tener permisos restringidos. No se debe eliminar `terraform.tfstate` ni ejecutar `terraform destroy` sin revisar las consecuencias.

El workflow verifica que los cambios afecten solo a los recursos Terraform definidos para esta aplicación y rechaza cualquier plan que elimine el volumen persistente.

## Verificar despliegue

El workflow valida la salud de la API después del despliegue. Para verificarla de manera adicional desde el propio VPS:

~~~bash
curl -fsS http://127.0.0.1:8088/actuator/health
docker ps --filter name=gestor-franquicias
cd /opt/gestor-franquicias
docker compose -p gestor-franquicias --env-file .env -f docker-compose.prod.yml ps
~~~

Swagger UI estará disponible dentro del servidor en `http://127.0.0.1:8088/swagger-ui.html`, mientras el puerto siga ligado a localhost.

La API **no se expone a Internet automáticamente** porque el enunciado no requiere autenticación. Para que un evaluador remoto acceda a Swagger, se debe configurar primero un proxy HTTPS o una regla de firewall que limite las direcciones autorizadas. Los puertos 80 y 443 pueden estar en uso por otros servicios; no se modifican aquí.

## Limitaciones y medidas de seguridad

- La conexión SSH y los secretos deben configurarse una sola vez. Sin estos accesos la automatización no puede modificar el VPS.
- Una contraseña `DB_PASSWORD` ya utilizada debe conservarse entre despliegues; cambiarla en el archivo de entorno no cambia automáticamente la contraseña interna de PostgreSQL.
- No se permite eliminar el volumen de PostgreSQL mediante este flujo. Hacer copias de seguridad antes de actualizar sistemas con datos relevantes.
- La publicación en GHCR y el despliegue en el VPS son acciones separadas; publicar una imagen no significa que la API ya esté desplegada.
- El estado Terraform está almacenado localmente en el VPS, no en GitHub Actions. Es necesario conservarlo y respaldarlo.

## Retirar la instalación

La eliminación de recursos requiere una intervención explícita y una comprobación del estado de PostgreSQL. No existe un workflow de eliminación automática. Nunca ejecutar comandos generales como `docker system prune --volumes` en este servidor compartido.

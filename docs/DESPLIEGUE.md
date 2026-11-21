# Despliegue y automatización

La aplicación usa **GitHub Actions** para compilar, ejecutar pruebas y publicar imágenes Docker en GHCR. Cuando la validación de `main` finaliza correctamente, el despliegue de producción se ejecuta de forma automática en el VPS.

## Ambientes

Las cuatro configuraciones se validan durante la integración continua. En el VPS **solo está desplegado `prod`**. Los puertos de los otros perfiles están definidos para posibles despliegues independientes; no son servicios activos en ese servidor.

| Perfil | Rama | Puerto configurado | Estado en el VPS |
| --- | --- | --- | --- |
| `dev` | `dev` | 8086 | No desplegado |
| `test` | `test` | 8087 | No desplegado |
| `stage` | `stage` | 8089 | No desplegado |
| `prod` | `main` | 8088 | Desplegado |

La dirección de producción es http://2.25.123.13:8088/swagger-ui/index.html. PostgreSQL permanece dentro de una red Docker privada.

## Flujo automático

```text
Push / integración en main
          |
          v
Validación backend (Java 21, JUnit, Testcontainers, Docker y Terraform)
          |
          v
Publicar imagen Docker (GHCR, etiqueta con revisión del código)
          |
          v
Desplegar producción (SSH, Terraform y Docker Compose)
          |
          v
Comprobar salud, OpenAPI, Swagger y consulta de franquicias
```

El workflow de publicación conserva la referencia exacta de la revisión validada. En `main` genera también la etiqueta `latest`, aunque el despliegue usa la etiqueta de la revisión concreta y no `latest`.

Los cambios en `dev`, `test` y `stage` pueden generar imágenes en GHCR si pasan CI, pero **no activan el despliegue automático del VPS**. Las ramas `feature/*` y `bugfix/*` ejecutan validaciones, sin desplegar producción.

## Workflows y scripts

### GitHub Actions

| Archivo | Ejecución | Función |
| --- | --- | --- |
| [`ci.yml`](../.github/workflows/ci.yml) | Push a ramas de desarrollo y permanentes, o ejecución manual | Ejecuta `mvn clean verify`, construye `Dockerfile.runtime`, inicia cada perfil en contenedores temporales con PostgreSQL y valida Terraform. |
| [`validar-ejecucion-local.yml`](../.github/workflows/validar-ejecucion-local.yml) | Push a `main` o ejecución manual | Prueba desde cero Docker Compose y Maven con PostgreSQL, revisa el script PowerShell, los ejemplos JSON, la salud, Swagger, consultas y creación de franquicias. |
| [`publicar-imagen.yml`](../.github/workflows/publicar-imagen.yml) | Después de `Validación backend` exitosa o manualmente | Publica en GHCR. Solo una validación exitosa originada en `main` activa automáticamente el despliegue productivo. |
| [`_desplegar-ambiente.yml`](../.github/workflows/_desplegar-ambiente.yml) | Workflow reutilizable | Verifica secretos, prepara SSH, empaqueta los archivos del ambiente, crea su `.env` temporalmente desde secretos, ejecuta Terraform/Docker en el VPS y realiza comprobaciones HTTP. |
| [`desplegar-dev.yml`](../.github/workflows/desplegar-dev.yml) | Manual desde `dev` | Preparado para desplegar el perfil de desarrollo, si se habilita su entorno. |
| [`desplegar-test.yml`](../.github/workflows/desplegar-test.yml) | Manual desde `test` | Preparado para desplegar el perfil de pruebas. |
| [`desplegar-stage.yml`](../.github/workflows/desplegar-stage.yml) | Manual desde `stage` | Preparado para desplegar el perfil de preproducción. |
| [`desplegar-produccion.yml`](../.github/workflows/desplegar-produccion.yml) | Manual desde `main` | Permite ejecutar nuevamente el despliegue de producción. |

Los workflows manuales de desarrollo, pruebas y preproducción forman parte de la configuración, **pero no se han ejecutado para desplegar dichos ambientes en el VPS de esta entrega**. La ejecución manual de publicación de imagen tampoco inicia automáticamente producción.

### Scripts

| Archivo | Función |
| --- | --- |
| [`scripts/validar-contenedor.sh`](../scripts/validar-contenedor.sh) | Crea contenedores temporales de API y PostgreSQL por perfil, prueba R2DBC/Flyway, crea y consulta una franquicia, reinicia la API para comprobar persistencia y limpia los recursos. |
| [`scripts/desplegar-vps.sh`](../scripts/desplegar-vps.sh) | Valida el ambiente y las variables del VPS, inicializa Terraform, revisa el plan antes de aplicarlo, levanta PostgreSQL, descarga la imagen GHCR y ejecuta Docker Compose. Comprueba el contenedor y el endpoint de salud. |
| [`scripts/iniciar-local-maven.sh`](../scripts/iniciar-local-maven.sh) | Inicia PostgreSQL en Docker y ejecuta Spring Boot con Maven en Linux/macOS, sin requerir `.env`. |
| [`scripts/iniciar-local-maven.ps1`](../scripts/iniciar-local-maven.ps1) | Hace el mismo arranque local desde Windows PowerShell. |

### Docker y Terraform

- [`Dockerfile`](../Dockerfile): construcción multietapa utilizada por `docker compose up -d --build` en equipos locales.
- [`Dockerfile.runtime`](../Dockerfile.runtime): empaqueta el JAR previamente compilado por el CI y evita volver a descargar dependencias al publicar.
- [`docker-compose.yml`](../docker-compose.yml): API y PostgreSQL para uso local; expone solo `127.0.0.1:8088` y `127.0.0.1:5434`. Incluye credenciales predeterminadas de demostración.
- `docker-compose.dev.yml`, `docker-compose.test.yml`, `docker-compose.stage.yml` y `docker-compose.prod.yml`: configuración de perfiles y conexión a los recursos aislados del ambiente correspondiente.
- [`infra/terraform/main.tf`](../infra/terraform/main.tf): administra red, volumen y contenedor PostgreSQL; no crea el VPS. Los estados se guardan por separado en cada directorio del servidor.

## Configuración de producción en GitHub

En `Settings → Environments`, el ambiente `produccion` utiliza la rama `main`. Configurar estos secretos:

| Secreto | Para qué se utiliza |
| --- | --- |
| `VPS_HOST` | Dirección del servidor |
| `VPS_USER` | Usuario autorizado para SSH |
| `VPS_SSH_KEY` | Clave privada de acceso SSH |
| `VPS_KNOWN_HOSTS` | Firma del host, verificada previamente |
| `DB_PASSWORD` | Contraseña propia de PostgreSQL de producción |

Cuando el paquete de GHCR no es público, se pueden configurar `GHCR_USUARIO` y `GHCR_LEER_PAQUETE`. La variable de entorno `VPS_PUBLIC_HOST` permite cambiar la dirección que utiliza el CI para las comprobaciones externas.

**La contraseña local predeterminada de `docker-compose.yml` no se usa en producción.** El despliegue del VPS recibe sus credenciales desde los secretos del entorno y genera allí el archivo `.env` con permisos restringidos. No guardar claves SSH, estados de Terraform, contraseñas reales ni `.env` de producción en Git.

## Procedimiento en el VPS

El proyecto se instala en `/opt/gestor-franquicias/prod`. El despliegue sigue estos pasos:

1. GitHub Actions comprueba la conexión SSH y envía únicamente los archivos necesarios.
2. `scripts/desplegar-vps.sh` valida el perfil `prod`, el puerto 8088 y la imagen exacta publicada.
3. Terraform crea o actualiza los recursos de PostgreSQL. El script rechaza planes que intenten eliminar el volumen de datos.
4. Docker Compose inicia la API usando la imagen descargada desde GHCR.
5. El servidor comprueba `/actuator/health`, la red Docker y la imagen activa.
6. GitHub Actions verifica desde Internet `/actuator/health`, `/v3/api-docs`, Swagger y `GET /api/v1/franquicias`.

Para consultar el estado directamente en el VPS:

```bash
docker ps --filter name=gestor-franquicias-prod
curl -fsS http://127.0.0.1:8088/actuator/health
```

La API productiva escucha en `0.0.0.0:8088`. El despliegue abre el puerto TCP 8088 en UFW si está activo. Si el proveedor controla un firewall adicional, debe permitir ese puerto. PostgreSQL no publica su puerto a Internet.

**Precaución:** la API pública utiliza HTTP y los endpoints de escritura no exigen autenticación. Se recomienda usar únicamente datos ficticios y añadir HTTPS y controles de acceso antes de trabajar con datos reales.

## Desarrollo local

No hace falta configurar secretos, Terraform ni acceso SSH para ejecutar el proyecto localmente.

```bash
docker compose up -d --build
```

Este comando usa credenciales de demostración definidas en `docker-compose.yml` si no existe `.env`.

Para iniciar el backend mediante Maven y dejar PostgreSQL en Docker:

- Windows PowerShell: `.\scripts\iniciar-local-maven.ps1`
- Linux/macOS: `bash scripts/iniciar-local-maven.sh`

Los requisitos, las comprobaciones y la solución de problemas se encuentran en el [README principal](../README.md#ejecutar-localmente). Las solicitudes de ejemplo están en [Ejemplos de la API](EJEMPLOS_API.md).

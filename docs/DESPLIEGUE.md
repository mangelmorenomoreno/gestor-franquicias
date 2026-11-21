# Despliegue por ambientes mediante GitHub Actions

La API Gestor de Franquicias dispone de cuatro ambientes aislados para probar cambios sin modificar la base de datos de producción. Todos utilizan Java 21, Spring Boot, R2DBC, Flyway, Docker y Terraform. El aprovisionamiento en el VPS es **manual mediante GitHub Actions**: no se ejecuta por un push ni por un merge. Los despliegues al VPS aún no se han realizado.

## Ambientes, conexiones y perfiles

| Ambiente | Rama | Entorno GitHub | Perfil Spring | Puerto local VPS | Directorio del VPS |
|---|---|---|---|---|---|
| Desarrollo | `dev` | `desarrollo` | `dev` | 8086 | `/opt/gestor-franquicias/dev` |
| Pruebas | `test` | `pruebas` | `test` | 8087 | `/opt/gestor-franquicias/test` |
| Preproducción | `stage` | `preproduccion` | `stage` | 8089 | `/opt/gestor-franquicias/stage` |
| Producción | `main` | `produccion` | `prod` | 8088 | `/opt/gestor-franquicias/prod` |

**Primero se configura y verifica `dev`; los demás ambientes se activan después de aprobar sus comprobaciones.**

Cada ambiente conserva su propio estado Terraform, red Docker, contenedor PostgreSQL 16, volumen persistente y contenedor Spring Boot. Todos pueden llamar a su base `gestor_franquicias` porque son instancias independientes; la dirección `base-datos` se resuelve únicamente dentro de la red correspondiente.

| Perfil | Red privada | Contenedor PostgreSQL | Volumen de datos |
|---|---|---|---|
| dev | `gestor-franquicias-dev-red` | `gestor-franquicias-dev-postgres` | `gestor-franquicias-dev-datos` |
| test | `gestor-franquicias-test-red` | `gestor-franquicias-test-postgres` | `gestor-franquicias-test-datos` |
| stage | `gestor-franquicias-stage-red` | `gestor-franquicias-stage-postgres` | `gestor-franquicias-stage-datos` |
| prod | `gestor-franquicias-prod-red` | `gestor-franquicias-prod-postgres` | `gestor-franquicias-prod-datos` |

Los archivos `src/main/resources/application-dev.yml`, `application-test.yml`, `application-stage.yml` y `application-prod.yml` personalizan Spring Boot. Los archivos `docker-compose.dev.yml`, `docker-compose.test.yml`, `docker-compose.stage.yml` y `docker-compose.prod.yml` inyectan `SPRING_PROFILES_ACTIVE`, las URLs de R2DBC y Flyway, el usuario y la contraseña para la base de su propia red.

## Flujo correcto

La integración sigue `feature/* → dev → test → stage → main` mediante Pull Requests. El workflow `Validación backend` ejecuta las pruebas con Testcontainers y arranca la aplicación en Docker con los perfiles `dev`, `test`, `stage` y `prod` para comprobar la conexión, Flyway, lectura, escritura y persistencia.

El workflow `Publicar imagen Docker` publica una etiqueta inmutable con el SHA del commit de cada rama permanente **solamente después de CI exitoso**. La etiqueta `latest` se actualiza únicamente desde `main`; no se utiliza para despliegues.

La primera vez que se habilite esta configuración, puede que una rama todavía no tenga su imagen publicada con el SHA actual. En ese caso se debe ejecutar una sola vez `Actions → Publicar imagen Docker → Run workflow` seleccionando dicha rama. La publicación manual también ejecuta las pruebas Maven antes de subir la imagen; en integraciones posteriores la publicación ocurre automáticamente tras CI exitoso.

Los cuatro workflows de despliegue manual son:

- `Desplegar dev en el VPS` desde la rama `dev`.
- `Desplegar test en el VPS` desde la rama `test`.
- `Desplegar stage en el VPS` desde la rama `stage`.
- `Desplegar prod en el VPS` desde la rama `main`.

Todos llaman al workflow reutilizable `_desplegar-ambiente.yml`, que valida secretos, transfiere únicamente los archivos del ambiente seleccionado y ejecuta `scripts/desplegar-vps.sh` con el nombre de dicho ambiente.

## Configuración inicial de secretos

Crear los ambientes en `Settings → Environments`: `desarrollo`, `pruebas`, `preproduccion` y `produccion`. Asignar a cada uno su rama autorizada y sus secretos:

| Secreto | Contenido |
|---|---|
| `VPS_HOST` | IP o DNS auténtico del VPS |
| `VPS_USER` | Usuario SSH autorizado para desplegar |
| `VPS_SSH_KEY` | Clave privada SSH de ese usuario |
| `VPS_KNOWN_HOSTS` | Llave pública de host verificada antes de guardarla |
| `DB_PASSWORD` | Contraseña distinta por ambiente, mínimo 20 caracteres alfanuméricos, guiones o guiones bajos |

Si GHCR es privado, también configurar `GHCR_USUARIO` y `GHCR_LEER_PAQUETE` (token con `read:packages`). Las credenciales SSH pueden ser las mismas si se utiliza el mismo VPS, pero **las contraseñas de base de datos deben ser diferentes**.

Para generar una clave segura en una terminal privada:

~~~bash
openssl rand -hex 32
~~~

No subir contraseñas, llaves SSH privadas ni archivos `.env` a Git ni compartirlos en la conversación.

## Primer despliegue: desarrollo

1. Revisar que el CI de `dev` y la publicación de su SHA en GHCR finalizaron correctamente.
2. Configurar los secretos del entorno GitHub `desarrollo` y comprobar el acceso SSH sin compartir contraseñas.
3. En GitHub abrir `Actions → Desplegar dev en el VPS → Run workflow` y seleccionar la rama `dev`.
4. Confirmar que la ejecución instala Terraform si hace falta, inicializa el estado en `/opt/gestor-franquicias/dev/infra/terraform`, aplica el plan validado, conecta Spring Boot al PostgreSQL de `dev` y obtiene `UP` en `/actuator/health`.
5. Revisar Swagger desde el VPS en `http://127.0.0.1:8086/swagger-ui.html`. El puerto no se publica por Internet por defecto.

Para consultar sin cambiar nada:

~~~bash
curl -fsS http://127.0.0.1:8086/actuator/health
docker ps --filter name=gestor-franquicias-dev
~~~

Después de validar desarrollo, configurar y ejecutar `test`, `stage` y `prod` en ese orden, cada uno mediante su propio workflow y su etiqueta de imagen.

## Aislamiento, estado y seguridad

El despliegue crea los archivos en `/opt/gestor-franquicias/<ambiente>` y guarda el estado Terraform por separado. El plan impide eliminar volúmenes PostgreSQL y rechaza cambios a direcciones de recursos que no pertenezcan a la configuración. No se ejecuta `terraform destroy` automáticamente.

Terraform se instalará desde el repositorio oficial firmado si no está disponible. Si la base ya tiene información, la automatización rechaza modificar la contraseña PostgreSQL por accidente. Los archivos `.env` y `terraform.tfstate` son privados; hacer copia de seguridad del estado y de los datos antes de operaciones críticas.

Los cuatro puertos del VPS se enlazan inicialmente a `127.0.0.1`, evitando exponer una API sin autenticación. Para dar acceso remoto a Swagger hay que configurar un proxy seguro o acceso restringido por firewall. No se modifican los puertos ni los contenedores de otras aplicaciones alojadas en el servidor.

**Estado del despliegue:** los recursos, scripts y workflows se preparan en el repositorio, pero todavía se requiere conectar GitHub al VPS y ejecutar el primer despliegue real. No se debe afirmar que los cuatro ambientes están instalados hasta verificar sus resultados.

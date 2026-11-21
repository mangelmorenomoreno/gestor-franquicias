# Despliegue por ambientes mediante GitHub Actions

La API Gestor de Franquicias dispone de cuatro ambientes aislados para probar cambios sin modificar la base de datos de producción. Todos utilizan Java 21, Spring Boot, R2DBC, Flyway, Docker y Terraform. Los ambientes se prueban por separado en GitHub Actions, pero **solo producción se instala en el VPS**. Los cambios integrados en `main` disparan CI; cuando pasa, se publica en GHCR la imagen del commit validado y se despliega `prod` automáticamente. Abrir un PR no despliega. **Hay evidencia de una ejecución exitosa:** [#44859340375](https://github.com/mangelmorenomoreno/gestor-franquicias/actions/runs/44859340375), commit `33001291d0e031838af99171d5882cd793c29d87`.

## Ambientes, conexiones y perfiles

**Importante: los puertos de esta tabla son valores definidos en los archivos de configuración, NO servicios que estén ejecutándose en el VPS.** La única instancia desplegada en el servidor es producción (`main` / perfil `prod`, puerto público `8088`). Los demás perfiles se validan en CI usando contenedores temporales y no están publicados ni disponibles en los puertos `8086`, `8087` o `8089` del VPS por este proceso.

| Ambiente | Rama | Perfil Spring | Puerto reservado en configuración | Estado del despliegue en el VPS |
|---|---|---|---|---|
| Desarrollo | `dev` | `dev` | `8086` | **No desplegado**. Solo validación temporal en GitHub Actions |
| Pruebas | `test` | `test` | `8087` | **No desplegado**. Solo validación temporal en GitHub Actions |
| Preproducción | `stage` | `stage` | `8089` | **No desplegado**. Solo validación temporal en GitHub Actions |
| Producción | `main` | `prod` | `8088` | **Desplegado y verificado**: `http://2.25.123.13:8088` |

El directorio efectivo del despliegue de producción es `/opt/gestor-franquicias/prod`. Los directorios `/opt/gestor-franquicias/dev`, `/opt/gestor-franquicias/test` y `/opt/gestor-franquicias/stage` son rutas previstas por los scripts para una eventual instalación manual, **no evidencia de que existan instancias desplegadas**. Las configuraciones de los cuatro ambientes se mantienen para reproducibilidad y pruebas, pero no se levantan simultáneamente en el VPS.

Cada perfil está preparado para conservar su propio estado Terraform, red Docker, contenedor PostgreSQL 16, volumen persistente y contenedor Spring Boot; en el VPS de esta entrega únicamente se instala producción. Todos pueden llamar a su base `gestor_franquicias` porque son instancias independientes; la dirección `base-datos` se resuelve únicamente dentro de la red correspondiente.

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

La secuencia automática es **merge a main → validación backend exitosa → publicación GHCR exitosa → despliegue prod → healthcheck**. El job `desplegar_produccion` se ejecuta tras `publicar`, solo si la validación original corresponde a la rama `main`. Se pasa al despliegue el SHA exacto de esa validación para evitar usar un commit distinto. Ni un PR abierto ni una publicación manual desencadenan automáticamente el despliegue.

Existen workflows manuales preparados para cada perfil, pero **no forman parte del despliegue realizado**: los ambientes `dev`, `test` y `stage` no se han instalado en el VPS para esta entrega. El único despliegue ejecutado y verificado es `prod` desde `main`. Los workflows disponibles en el repositorio son:

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

## Despliegue de producción

1. En GitHub Environments, configurar `produccion` con acceso desde `main` y sus secretos SSH/PostgreSQL.
2. Crear un PR a `main`, revisarlo y fusionarlo. El push del merge activa el CI. Ante un error, no se publicará ni desplegará.
3. El workflow `Publicar imagen Docker` publica el SHA validado en GHCR y, si corresponde a `main`, llama al despliegue de producción.
4. Terraform inicializa la red, volumen y PostgreSQL exclusivos de `prod`; la API se enlaza a `0.0.0.0:8088`, para aceptar conexiones desde la IP pública del VPS.
5. El despliegue comprueba `/actuator/health`, conexión R2DBC y migraciones Flyway; desde GitHub Actions consulta además la IP pública para verificar `/actuator/health`, `/v3/api-docs`, `/swagger-ui/index.html` y `GET /api/v1/franquicias`. También valida que el contenedor esté ejecutando la imagen etiquetada con el commit verificado.

Para relanzar manualmente sin nuevo commit: `Actions → Desplegar prod en el VPS → Run workflow` (rama `main`). Desde el VPS:

~~~bash
curl -fsS http://127.0.0.1:8088/actuator/health
docker ps --filter name=gestor-franquicias-prod
~~~

El archivo de claves públicas de HashiCorp se instala con permisos `0644` para que `apt` pueda verificar su firma, aunque el resto del despliegue mantenga `umask 077` para proteger secretos. Si una instalación anterior dejó ese archivo con permisos restrictivos, se reparan también antes del primer `apt-get update` del siguiente intento.

## Aislamiento, estado y seguridad

El despliegue realizado creó los recursos de producción en `/opt/gestor-franquicias/prod`; los scripts soportan rutas por perfil para posibles despliegues futuros. No se afirma que los demás perfiles estén instalados en el VPS. Para producción, Terraform guarda su estado por separado. El plan impide eliminar volúmenes PostgreSQL y rechaza cambios a direcciones de recursos que no pertenezcan a la configuración. No se ejecuta `terraform destroy` automáticamente.

Terraform se instalará desde el repositorio oficial firmado si no está disponible. Si la base ya tiene información, la automatización rechaza modificar la contraseña PostgreSQL por accidente. Los archivos `.env` y `terraform.tfstate` son privados; hacer copia de seguridad del estado y de los datos antes de operaciones críticas.

Los perfiles `dev`, `test` y `stage` mantienen `127.0.0.1`. Solo `prod` publica TCP `8088` en todas las interfaces (`0.0.0.0`) y abre ese puerto en UFW cuando está activo. En un proveedor con firewall externo, permitir adicionalmente TCP `8088` de entrada. PostgreSQL continúa exclusivamente en la red Docker privada; no se publican puertos de la base de datos ni se modifican contenedores de otras aplicaciones.

**Acceso público comprobado en la ejecución #44859340375:** [Swagger UI](http://2.25.123.13:8088/swagger-ui/index.html), [OpenAPI](http://2.25.123.13:8088/v3/api-docs), [salud](http://2.25.123.13:8088/actuator/health) y [GET franquicias](http://2.25.123.13:8088/api/v1/franquicias). La variable opcional `VPS_PUBLIC_HOST` (GitHub Actions → Variables) reemplaza la IP de la prueba externa si se migra el servidor; su valor predeterminado en el workflow es `2.25.123.13`. La IP que muestra Docker (`0.0.0.0`) indica escucha en todas las interfaces, no es una dirección para el navegador. Verificar desde una red externa, porque el chequeo local del VPS no demuestra conectividad externa.

**Advertencia:** esta prueba técnica no incorpora autenticación en los endpoints de escritura, por lo que abrir `8088` permite que cualquiera en Internet consulte, cree, modifique o elimine datos expuestos por la API. No almacenar datos reales/sensibles. Para un uso prolongado se recomienda HTTPS con proxy inverso, autenticación y limitación de peticiones. HTTP directo por IP se habilita únicamente a petición para la evaluación.

**Historial de resolución:** el intento `#44855855301` falló por permisos del keyring de HashiCorp; posteriormente se corrigió la instalación. El despliegue automatizado [#44859340375](https://github.com/mangelmorenomoreno/gestor-franquicias/actions/runs/44859340375) terminó con `success` en publicación, despliegue y pruebas HTTP desde Internet el 20 de noviembre de 2026, hora de Colombia, con imagen `ghcr.io/mangelmorenomoreno/gestor-franquicias:33001291d0e031838af99171d5882cd793c29d87`. No extrapolar ese resultado a commits posteriores sin sus propios registros.


## Cómo reproducir la aplicación sin acceso al VPS

La entrega puede evaluarse íntegramente en cualquier equipo con Git y Docker Compose:

~~~bash
git clone https://github.com/mangelmorenomoreno/gestor-franquicias.git
cd gestor-franquicias
cp .env.example .env
# Cambiar DB_PASSWORD por una contraseña local privada
docker compose up -d --build
curl -fsS http://localhost:8088/actuator/health
curl -fsS http://localhost:8088/api/v1/franquicias
~~~

En Windows PowerShell utilice `Copy-Item .env.example .env` en lugar de `cp`. Abra `http://localhost:8088/swagger-ui/index.html` para probar los endpoints. PostgreSQL local escucha en `127.0.0.1:5434`; el Compose local no depende de Terraform ni de los secretos SSH de GitHub. Para ejecutar la API con Java 21 y Maven, utilizar la alternativa documentada en [README, apartado 8](../README.md#8-ejecución-local).

**Limitación relevante:** el acceso HTTP público es para demostración, no una configuración de producción endurecida. La API carece de autenticación sobre endpoints de escritura. No introducir datos sensibles; habilitar HTTPS y control de acceso antes de almacenar información real.

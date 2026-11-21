# Despliegue por ambientes mediante GitHub Actions

La API Gestor de Franquicias dispone de cuatro ambientes aislados para probar cambios sin modificar la base de datos de producción. Todos utilizan Java 21, Spring Boot, R2DBC, Flyway, Docker y Terraform. Los ambientes se prueban por separado en GitHub Actions, pero **solo producción se instala en el VPS**. Cada PR fusionado a `main` inicia CI; si este pasa, se publica la imagen Docker del commit validado y se despliega `prod` automáticamente. Abrir un PR no despliega.

## Ambientes, conexiones y perfiles

| Ambiente | Rama | Entorno GitHub | Perfil Spring | Puerto local VPS | Directorio del VPS |
|---|---|---|---|---|---|
| Desarrollo | `dev` | `desarrollo` | `dev` | 8086 | `/opt/gestor-franquicias/dev` |
| Pruebas | `test` | `pruebas` | `test` | 8087 | `/opt/gestor-franquicias/test` |
| Preproducción | `stage` | `preproduccion` | `stage` | 8089 | `/opt/gestor-franquicias/stage` |
| Producción | `main` | `produccion` | `prod` | 8088 | `/opt/gestor-franquicias/prod` |

**En el VPS únicamente se despliega `prod`.** Los perfiles de `dev`, `test` y `stage` siguen probándose en CI y no se instalan en el servidor en este flujo.

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

La secuencia automática es **merge a main → validación backend exitosa → publicación GHCR exitosa → despliegue prod → healthcheck**. El job `desplegar_produccion` se ejecuta tras `publicar`, solo si la validación original corresponde a la rama `main`. Se pasa al despliegue el SHA exacto de esa validación para evitar usar un commit distinto. Ni un PR abierto ni una publicación manual desencadenan automáticamente el despliegue.

Siguen existiendo workflows manuales, pero en este servidor únicamente debe ejecutarse producción. Los workflows disponibles son:

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
5. El despliegue verifica `/actuator/health`, conexión R2DBC y migraciones Flyway.

Para relanzar manualmente sin nuevo commit: `Actions → Desplegar prod en el VPS → Run workflow` (rama `main`). Desde el VPS:

~~~bash
curl -fsS http://127.0.0.1:8088/actuator/health
docker ps --filter name=gestor-franquicias-prod
~~~

El archivo de claves públicas de HashiCorp se instala con permisos `0644` para que `apt` pueda verificar su firma, aunque el resto del despliegue mantenga `umask 077` para proteger secretos. Si una instalación anterior dejó ese archivo con permisos restrictivos, se reparan también antes del primer `apt-get update` del siguiente intento.

## Aislamiento, estado y seguridad

El despliegue crea los archivos en `/opt/gestor-franquicias/<ambiente>` y guarda el estado Terraform por separado. El plan impide eliminar volúmenes PostgreSQL y rechaza cambios a direcciones de recursos que no pertenezcan a la configuración. No se ejecuta `terraform destroy` automáticamente.

Terraform se instalará desde el repositorio oficial firmado si no está disponible. Si la base ya tiene información, la automatización rechaza modificar la contraseña PostgreSQL por accidente. Los archivos `.env` y `terraform.tfstate` son privados; hacer copia de seguridad del estado y de los datos antes de operaciones críticas.

Los perfiles `dev`, `test` y `stage` mantienen `127.0.0.1`. Solo `prod` publica TCP `8088` en todas las interfaces (`0.0.0.0`) y abre ese puerto en UFW cuando está activo. En un proveedor con firewall externo, permitir adicionalmente TCP `8088` de entrada. PostgreSQL continúa exclusivamente en la red Docker privada; no se publican puertos de la base de datos ni se modifican contenedores de otras aplicaciones.

**Acceso público:** `http://IP_PUBLICA_DEL_VPS:8088/swagger-ui.html`, especificación en `http://IP_PUBLICA_DEL_VPS:8088/v3/api-docs` y comprobación `http://IP_PUBLICA_DEL_VPS:8088/actuator/health`. La IP que muestra Docker (`0.0.0.0`) indica escucha en todas las interfaces, no es una dirección para el navegador. Verificar desde una red externa, porque el chequeo local del VPS no demuestra conectividad externa.

**Advertencia:** esta prueba técnica no incorpora autenticación en los endpoints de escritura, por lo que abrir `8088` permite que cualquiera en Internet consulte, cree, modifique o elimine datos expuestos por la API. No almacenar datos reales/sensibles. Para un uso prolongado se recomienda HTTPS con proxy inverso, autenticación y limitación de peticiones. HTTP directo por IP se habilita únicamente a petición para la evaluación.

**Registro:** el intento manual `#44855855301` alcanzó el VPS y falló al instalar Terraform porque `apt` no podía leer el keyring de HashiCorp. El arreglo y el despliegue automático solo se considerarán verificados después de una nueva ejecución exitosa. No se asume que PostgreSQL o la API estén instalados hasta comprobarlo.

# Gestor de Franquicias

API REST para administrar franquicias, sucursales, productos y existencias por sucursal.

Desarrollada con **Java 21, Spring Boot 3.5, WebFlux, R2DBC y PostgreSQL 16**. Incluye migraciones con Flyway, pruebas con JUnit/Testcontainers, Docker y Terraform.

## Ejecutar localmente

Hay dos formas de iniciar el proyecto. Ambas utilizan PostgreSQL en Docker; la diferencia es dónde se ejecuta Spring Boot.

**Requisitos generales:** Git y Docker Desktop (Windows/macOS) o Docker Engine con Compose (Linux), con Docker iniciado.

### Opción 1: Docker Compose (API y PostgreSQL)

No necesita instalar Java ni Maven.

```bash
git clone https://github.com/mangelmorenomoreno/gestor-franquicias.git
cd gestor-franquicias
```

Crear el archivo de variables:

**Windows (PowerShell):**
```powershell
Copy-Item .env.example .env
```

**Linux/macOS:**
```bash
cp .env.example .env
```

Editar `.env` y establecer una contraseña local en `DB_PASSWORD`. Mantener el usuario `DB_USERNAME=gestor_franquicias`.

Iniciar los contenedores:

```bash
docker compose up -d --build
docker compose ps
```

Consultar la API:

- Swagger: http://localhost:8088/swagger-ui/index.html
- Salud: http://localhost:8088/actuator/health
- OpenAPI: http://localhost:8088/v3/api-docs
- Franquicias: http://localhost:8088/api/v1/franquicias

Docker publica la API en el puerto **8088** y PostgreSQL en el **5434**, ambos solo en `127.0.0.1`.

Para revisar logs y detener los contenedores:

```bash
docker compose logs -f api
docker compose down
```

`docker compose down` conserva la información de la base de datos. El comando `docker compose down -v` también elimina el volumen y **borra los datos locales**.

### Opción 2: Spring Boot con Maven y PostgreSQL en Docker

Se necesita **JDK 21** y **Maven** además de Docker. Desde la raíz del repositorio, crear el archivo `.env` como se indica en la opción 1.

Si la API de Docker Compose ya estaba iniciada, detenerla:

```bash
docker compose stop api
docker compose up -d base-datos
```

**Windows (PowerShell):**
```powershell
$env:DB_URL = "r2dbc:postgresql://localhost:5434/gestor_franquicias"
$env:DB_MIGRATION_URL = "jdbc:postgresql://localhost:5434/gestor_franquicias"
$env:DB_USERNAME = "gestor_franquicias"
$env:DB_PASSWORD = ((Get-Content .env | Where-Object { $_ -match '^DB_PASSWORD=' } | Select-Object -First 1) -split '=', 2)[1]
mvn spring-boot:run
```

**Linux/macOS:**
```bash
set -a
source .env
set +a

export DB_URL="r2dbc:postgresql://localhost:5434/gestor_franquicias"
export DB_MIGRATION_URL="jdbc:postgresql://localhost:5434/gestor_franquicias"
mvn spring-boot:run
```

La API arranca en **http://localhost:8080** y Swagger está en http://localhost:8080/swagger-ui/index.html.

La contraseña del contenedor PostgreSQL y la utilizada por Maven deben coincidir. Si el volumen ya fue inicializado con otra contraseña, cambiar el archivo `.env` no modifica la contraseña existente en PostgreSQL.

### Comprobar el funcionamiento

Con la API iniciada, cambiar el puerto según la opción utilizada (8088 con Compose o 8080 con Maven):

```bash
curl -fsS http://localhost:8088/actuator/health
curl -fsS http://localhost:8088/api/v1/franquicias
curl -fsS http://localhost:8088/v3/api-docs
```

En Windows PowerShell también puede ejecutar las consultas con `curl.exe` (en lugar del alias `curl`).

El endpoint de salud debe responder con `"status":"UP"`. En una instalación nueva, la lista de franquicias puede estar vacía.

Para ejecutar las pruebas unitarias y de integración (requiere Docker activo para Testcontainers):

```bash
mvn clean verify
```

### Problemas habituales

- **Puerto ocupado:** revisar los puertos 8088 y 5434 para Compose, o 8080 para Maven.
- **Docker detenido:** comprobar con `docker version`.
- **Error de conexión a PostgreSQL:** revisar `docker compose logs base-datos`, las variables y el estado del contenedor.
- **Falla de inicio:** consultar `docker compose logs api` o los mensajes de Maven.
- **Contraseña incorrecta:** usar la que corresponde al volumen existente. Eliminar el volumen solo si se pueden descartar los datos de prueba.

## Funcionalidades

- Crear franquicias y asociarles sucursales.
- Crear productos con inventario por sucursal.
- Actualizar existencias y eliminar productos.
- Cambiar los nombres de franquicias, sucursales y productos.
- Consultar franquicias, sucursales y productos con sus relaciones.
- Obtener el producto con mayor stock por cada sucursal de una franquicia.

Todas las rutas REST comienzan con `/api/v1`:

| Método | Ruta | Operación |
| --- | --- | --- |
| GET | `/franquicias` | Listar franquicias |
| POST | `/franquicias` | Crear franquicia |
| GET | `/franquicias/{idFranquicia}` | Consultar franquicia y sucursales |
| GET | `/franquicias/{idFranquicia}/sucursales` | Listar sucursales de una franquicia |
| POST | `/franquicias/{idFranquicia}/sucursales` | Crear sucursal |
| PATCH | `/franquicias/{idFranquicia}` | Cambiar nombre de franquicia |
| GET | `/sucursales` | Listar sucursales |
| GET | `/sucursales/{idSucursal}` | Consultar sucursal y productos |
| GET | `/sucursales/{idSucursal}/productos` | Listar productos de sucursal |
| POST | `/sucursales/{idSucursal}/productos` | Crear producto |
| GET | `/sucursales/{idSucursal}/productos/{idProducto}` | Consultar producto |
| PATCH | `/sucursales/{idSucursal}/productos/{idProducto}` | Cambiar nombre de producto |
| PATCH | `/sucursales/{idSucursal}/productos/{idProducto}/stock` | Actualizar stock |
| DELETE | `/sucursales/{idSucursal}/productos/{idProducto}` | Eliminar producto |
| GET | `/productos` | Listar productos |
| GET | `/franquicias/{idFranquicia}/productos/mayor-stock` | Mayor stock por sucursal |

Ejemplo de creación:

```http
POST /api/v1/franquicias
Content-Type: application/json

{"nombre": "Franquicia Central"}
```

Las respuestas utilizan los campos `code`, `message`, `success`, `data`, `errors` y `timestamp`. Swagger contiene los contratos completos.

## Diseño

Cada sucursal pertenece a una franquicia y cada producto a una sucursal. El stock se administra por producto; el mismo nombre puede existir en diferentes sucursales. Los nombres se validan y las actualizaciones de productos utilizan un campo de versión para controlar modificaciones concurrentes.

El acceso a datos es reactivo mediante R2DBC. Los servicios utilizan **`Mono`** cuando la operación devuelve un único registro o ningún resultado, y **`Flux`** para consultas que devuelven varios registros. La consulta de mayor stock utiliza `ROW_NUMBER()` para escoger un producto por sucursal y resolver empates por identificador.

El código está organizado por funcionalidad, separando controladores, casos de uso, interfaces de acceso a datos, repositorios, entidades y DTO.

Más información en [Arquitectura](docs/ARQUITECTURA.md).

## Despliegue

La aplicación está desplegada en un VPS en el ambiente **`prod`**, desde la rama **`main`**.

- Swagger público: http://2.25.123.13:8088/swagger-ui/index.html
- API: http://2.25.123.13:8088/api/v1/franquicias

Los perfiles `dev`, `test` y `stage` se utilizan en las validaciones de integración continua; **no están desplegados en el VPS**. Los puertos indicados en sus archivos de configuración no representan servicios activos en el servidor.

El flujo de GitHub Actions compila, ejecuta pruebas, construye la imagen Docker, la publica en GHCR y despliega producción desde `main`. Terraform administra la red, el contenedor y el volumen de PostgreSQL de producción.

Configuración y mantenimiento: [Despliegue](docs/DESPLIEGUE.md) y [Terraform](infra/terraform/README.md).

**Nota:** el acceso público es HTTP y los endpoints de escritura no requieren autenticación. Utilizar únicamente datos de prueba.

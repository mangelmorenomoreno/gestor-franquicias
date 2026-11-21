# Gestor de Franquicias

API REST para administrar franquicias, sucursales, productos y existencias por sucursal.

Desarrollada con **Java 21, Spring Boot 3.5, WebFlux, R2DBC y PostgreSQL 16**. Incluye migraciones con Flyway, pruebas con JUnit/Testcontainers, Docker y Terraform.

**Documentación:** [Arquitectura](docs/ARQUITECTURA.md) · [Despliegue y GitHub Actions](docs/DESPLIEGUE.md) · [Ejemplos de los 17 endpoints](docs/EJEMPLOS_API.md) · [Terraform](infra/terraform/README.md).

## Ejecutar localmente

Hay dos formas de iniciar el proyecto. Ambas utilizan PostgreSQL en Docker; la diferencia es dónde se ejecuta Spring Boot.

**Requisitos generales:** Git y Docker Desktop (Windows/macOS) o Docker Engine con Compose (Linux), con Docker iniciado.

### Opción 1: Docker Compose (API y PostgreSQL)

No necesita instalar Java ni Maven.

```bash
git clone https://github.com/mangelmorenomoreno/gestor-franquicias.git
cd gestor-franquicias
```

Iniciar los contenedores directamente, sin crear archivos de configuración:

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

**Credenciales locales incluidas:** usuario `gestor_franquicias` y contraseña `franquicias_demo_local_2026`. Se usan únicamente para la base de desarrollo de Docker Compose, no para producción. No hace falta crear `.env`; si se desean credenciales diferentes, copiar `.env.example` a `.env` y editarlas antes de iniciar PostgreSQL por primera vez.

Para revisar logs y detener los contenedores:

```bash
docker compose logs -f api
docker compose down
```

`docker compose down` conserva la información de la base de datos. El comando `docker compose down -v` también elimina el volumen y **borra los datos locales**.

### Opción 2: Spring Boot con Maven y PostgreSQL en Docker

Requiere **JDK 21, Maven y Docker**. Después de clonar el repositorio, ejecute el script de su sistema operativo. Inicia PostgreSQL en Docker, configura las variables locales y ejecuta Spring Boot con Maven.

**Windows (PowerShell):**

```powershell
.\scripts\iniciar-local-maven.ps1
```

**Linux/macOS:**

```bash
bash scripts/iniciar-local-maven.sh
```

No se necesita `.env`. Si existe ese archivo con otras credenciales, el script las utiliza. En equipos que tengan restringidos los scripts de PowerShell puede utilizarse `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\iniciar-local-maven.ps1` desde la terminal local.

La API queda en **http://localhost:8080** y Swagger en http://localhost:8080/swagger-ui/index.html. El script detiene la API del Compose si está activa, pero deja PostgreSQL y sus datos disponibles.

Si se reutiliza un volumen creado con otra contraseña, hay que emplear la contraseña original o cambiarla desde PostgreSQL.

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

### Solicitudes y respuestas de ejemplo

Las rutas llevan el prefijo `/api/v1`. Los IDs son ilustrativos y `timestamp` se genera en UTC para cada solicitud.

**Crear franquicia**

```http
POST /api/v1/franquicias
Content-Type: application/json

{"nombre":"Franquicia Central"}
```

Respuesta `201`:

```json
{
  "code": "201",
  "message": "Franquicia creada correctamente.",
  "success": true,
  "data": {"idFranquicia": 1, "nombre": "Franquicia Central"},
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

**Crear sucursal**

```http
POST /api/v1/franquicias/1/sucursales
Content-Type: application/json

{"nombre":"Sucursal Norte"}
```

Respuesta `201`:

```json
{
  "code": "201",
  "message": "Sucursal creada correctamente.",
  "success": true,
  "data": {"idSucursal": 10, "nombre": "Sucursal Norte"},
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

**Crear producto**

```http
POST /api/v1/sucursales/10/productos
Content-Type: application/json

{"nombre":"Café","stock":20}
```

Respuesta `201`:

```json
{
  "code": "201",
  "message": "Producto creado correctamente.",
  "success": true,
  "data": {"idProducto": 100, "idSucursal": 10, "nombre": "Café", "stock": 20},
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

**Consultar el mayor stock por sucursal**

```http
GET /api/v1/franquicias/1/productos/mayor-stock
```

Respuesta `200` (dos sucursales):

```json
{
  "code": "200",
  "message": "Productos con mayor stock consultados correctamente.",
  "success": true,
  "data": [
    {"idSucursal": 10, "nombreSucursal": "Sucursal Norte", "idProducto": 100, "nombreProducto": "Café", "stock": 30},
    {"idSucursal": 11, "nombreSucursal": "Sucursal Sur", "idProducto": 103, "nombreProducto": "Agua", "stock": 45}
  ],
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

**Actualizar stock y eliminar producto**

```http
PATCH /api/v1/sucursales/10/productos/100/stock
Content-Type: application/json

{"stock":30}
```

Respuesta `200` con `data`:

```json
{"idProducto":100,"idSucursal":10,"nombre":"Café","stock":30}
```

`DELETE /api/v1/sucursales/10/productos/100` responde **`204 No Content`**, sin cuerpo.

Los ejemplos de **cada una de las 17 operaciones**, incluyendo los métodos `GET`, `POST`, `PATCH`, `DELETE` y sus respuestas, están en [Ejemplos de la API](docs/EJEMPLOS_API.md).

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

El flujo automático sigue el orden `main → pruebas → GHCR → VPS → comprobación HTTP`. Terraform administra la red, el contenedor y el volumen de PostgreSQL de producción.

| Archivo | Para qué sirve |
| --- | --- |
| [`ci.yml`](.github/workflows/ci.yml) | Compilación, pruebas, migraciones, validación Docker y Terraform. |
| [`validar-ejecucion-local.yml`](.github/workflows/validar-ejecucion-local.yml) | Inicia la aplicación con Docker Compose y con Maven, verificando ambas opciones. |
| [`publicar-imagen.yml`](.github/workflows/publicar-imagen.yml) | Publica en GHCR después de un CI exitoso; si viene de `main`, inicia producción. |
| [`_desplegar-ambiente.yml`](.github/workflows/_desplegar-ambiente.yml) | Configura SSH, transfiere los archivos necesarios y despliega en el VPS. |
| [`desplegar-*.yml`](docs/DESPLIEGUE.md#workflows-y-scripts) | Entradas manuales para cada perfil; solo `prod` está instalado en el VPS. |

Los scripts [`validar-contenedor.sh`](scripts/validar-contenedor.sh) y [`desplegar-vps.sh`](scripts/desplegar-vps.sh) validan los perfiles y despliegan la API, respectivamente. Para Maven local están [`iniciar-local-maven.sh`](scripts/iniciar-local-maven.sh) y [`iniciar-local-maven.ps1`](scripts/iniciar-local-maven.ps1).

La configuración, las etapas de cada workflow y los scripts se explican en [Despliegue y GitHub Actions](docs/DESPLIEGUE.md). Véanse también [Arquitectura](docs/ARQUITECTURA.md) y [Terraform](infra/terraform/README.md).

**Nota:** el acceso público es HTTP y los endpoints de escritura no requieren autenticación. Utilizar únicamente datos de prueba.

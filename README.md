# Gestor de Franquicias

API REST desarrollada como prueba técnica de Backend Developer. Administra franquicias, sucursales y productos, con control de inventario por sucursal y consulta del producto con mayor stock en cada una.

**Estado verificado (20 de noviembre de 2026, hora de Colombia):** el commit `33001291d0e031838af99171d5882cd793c29d87` se compiló y publicó en GHCR, se desplegó en el VPS y pasó las comprobaciones de la API, PostgreSQL, Swagger y OpenAPI desde un ejecutor externo de GitHub Actions. [Ver ejecución exitosa #44859340375](https://github.com/mangelmorenomoreno/gestor-franquicias/actions/runs/44859340375). Las versiones posteriores deben verificarse con sus propias ejecuciones.

**Acceso a la demostración (solo datos de prueba):**

- [Swagger UI público](http://2.25.123.13:8088/swagger-ui/index.html)
- [Especificación OpenAPI](http://2.25.123.13:8088/v3/api-docs)
- [Estado de salud](http://2.25.123.13:8088/actuator/health)
- [Consulta de franquicias](http://2.25.123.13:8088/api/v1/franquicias)

**Inicio rápido local (Docker):** `git clone https://github.com/mangelmorenomoreno/gestor-franquicias.git` → `cd gestor-franquicias` → copiar `.env.example` a `.env` y establecer una contraseña local → `docker compose up -d --build` → abrir [Swagger local](http://localhost:8088/swagger-ui/index.html). No requiere instalar Java en el equipo: Docker compila y ejecuta el backend. Para instrucciones de Windows, Linux/macOS, Maven y resolución de problemas, consultar [`8. Ejecución local`](#8-ejecución-local).

La aplicación utiliza Java 21, Spring Boot 3.5, WebFlux, R2DBC, PostgreSQL 16, Flyway, Docker, Terraform y GitHub Actions. Los perfiles lógicos `dev`, `test`, `stage` y `prod` se validan en CI; **solo `prod` se despliega en el VPS en el flujo automático**. Los demás perfiles no se exponen públicamente. Véase [Guía de despliegue](docs/DESPLIEGUE.md) y [Arquitectura](docs/ARQUITECTURA.md).

## 1. Funcionalidades

### Requisitos de la prueba

- Crear una franquicia.
- Consultar franquicias con sus sucursales y consultar sucursales con sus productos.
- Agregar sucursales a una franquicia existente.
- Agregar productos a una sucursal.
- Eliminar un producto de una sucursal.
- Modificar la cantidad de stock de un producto.
- Consultar el producto con mayor stock de cada sucursal de una franquicia, indicando a qué sucursal pertenece.
- Guardar la información en PostgreSQL alojado en un proveedor de nube.

### Funcionalidades adicionales

Se incluyeron los puntos extra planteados en la prueba:

- Cambiar el nombre de una franquicia.
- Cambiar el nombre de una sucursal.
- Cambiar el nombre de un producto.
- Empaquetar la aplicación con Docker.
- Trabajar con programación reactiva utilizando Spring WebFlux y R2DBC.
- Definir la infraestructura necesaria con Terraform.
- Desplegar la API y PostgreSQL en la nube.

También se incorporaron validaciones, manejo de errores, migraciones de base de datos, pruebas automatizadas y documentación OpenAPI/Swagger.

## 2. Tecnologías

| Componente | Tecnología |
| --- | --- |
| Lenguaje | Java 21 |
| Backend | Spring Boot |
| API REST | Spring WebFlux |
| Acceso a datos | Spring Data R2DBC |
| Base de datos | PostgreSQL |
| Migraciones | Flyway |
| Validaciones | Jakarta Validation |
| Documentación de endpoints | OpenAPI / Swagger |
| Pruebas | JUnit 5, Mockito, Reactor Test y Testcontainers |
| Contenedores | Docker y Docker Compose |
| Infraestructura como código | Terraform |
| Repositorio y automatización | GitHub y GitHub Actions |
| Despliegue | VPS Linux en la nube |

Se utiliza R2DBC para el acceso reactivo a PostgreSQL. Flyway manejará las migraciones de manera independiente usando una conexión JDBC para ese proceso.

## 3. Modelo de datos

Revisé nuevamente las relaciones para que las consultas siempre respeten a qué franquicia y sucursal pertenece cada registro. El documento describe una franquicia con varias sucursales y cada sucursal con sus propios productos.

### Relaciones

~~~text
franquicias
    id_franquicia (PK)
    nombre
         │
         │ 1 a muchos
         ▼
sucursales
    id_sucursal (PK)
    id_franquicia (FK)
    nombre
         │
         │ 1 a muchos
         ▼
productos
    id_producto (PK)
    id_sucursal (FK)
    nombre
    stock
    version
~~~

- **Franquicia → sucursales (1:N):** una franquicia puede tener cero, una o varias sucursales. Cada sucursal pertenece a una sola franquicia.
- **Sucursal → productos (1:N):** una sucursal puede tener cero, uno o varios productos. Cada producto registrado pertenece a una sola sucursal.
- **Franquicia → productos:** la relación se obtiene a través de las sucursales; no necesito guardar un `id_franquicia` adicional en la tabla de productos, porque ese dato ya está definido por la sucursal.
- **Stock:** se guarda en el producto de cada sucursal, no en la franquicia. El mismo nombre de producto puede aparecer en varias sucursales con cantidades diferentes.

No voy a crear una relación muchos a muchos ni un catálogo global de productos, porque la prueba solo pide productos ofertados en cada sucursal. Eso evitará duplicar relaciones sin una necesidad real.

### Estructura propuesta en PostgreSQL

Este modelo está definido en la migración `V1__crear_modelo.sql` de Flyway. Las pruebas de integración ejecutan las migraciones sobre PostgreSQL mediante Testcontainers.

~~~sql
CREATE TABLE franquicias (
    id_franquicia BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    CONSTRAINT chk_franquicias_nombre
        CHECK (LENGTH(BTRIM(nombre)) > 0)
);

CREATE TABLE sucursales (
    id_sucursal BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_franquicia BIGINT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    CONSTRAINT fk_sucursales_franquicia
        FOREIGN KEY (id_franquicia)
        REFERENCES franquicias (id_franquicia)
        ON DELETE RESTRICT,
    CONSTRAINT chk_sucursales_nombre
        CHECK (LENGTH(BTRIM(nombre)) > 0)
);

CREATE TABLE productos (
    id_producto BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_sucursal BIGINT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_productos_sucursal
        FOREIGN KEY (id_sucursal)
        REFERENCES sucursales (id_sucursal)
        ON DELETE RESTRICT,
    CONSTRAINT chk_productos_nombre
        CHECK (LENGTH(BTRIM(nombre)) > 0),
    CONSTRAINT chk_productos_stock
        CHECK (stock >= 0),
    CONSTRAINT chk_productos_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX uq_franquicias_nombre
    ON franquicias (LOWER(BTRIM(nombre)));

CREATE UNIQUE INDEX uq_sucursales_franquicia_nombre
    ON sucursales (id_franquicia, LOWER(BTRIM(nombre)));

CREATE UNIQUE INDEX uq_productos_sucursal_nombre
    ON productos (id_sucursal, LOWER(BTRIM(nombre)));

CREATE INDEX idx_productos_sucursal_stock
    ON productos (id_sucursal, stock DESC, id_producto ASC);
~~~

### Decisiones del modelo relacional

| Validación | Comportamiento |
| --- | --- |
| Sucursal sin franquicia válida | No se permite por clave foránea |
| Producto sin sucursal válida | No se permite por clave foránea |
| Dos sucursales con el mismo nombre dentro de una franquicia | No se permite |
| Dos sucursales con el mismo nombre en franquicias diferentes | Se permite |
| Dos productos con el mismo nombre en una sucursal | No se permite |
| Producto con el mismo nombre en sucursales diferentes | Se permite |
| Stock negativo | No se permite desde la API ni desde PostgreSQL |
| Nombre vacío o únicamente espacios | No se permite |
| Eliminar una franquicia o sucursal con dependencias | Se restringe para no borrar información relacionada por accidente |
| Actualizaciones simultáneas de stock | Se controlarán con una versión del registro y manejo de conflictos |

La unicidad de nombres y el control de versión son decisiones de diseño adicionales, no exigencias expresas de Accenture. Los nombres se normalizan y validan en la API y en la base de datos.

**No se incluyeron endpoints para eliminar franquicias o sucursales.** La prueba solo solicita eliminar productos. Por eso tampoco configuraré eliminaciones en cascada que puedan borrar inventario de manera accidental.

### Cómo se consultan las relaciones

Con R2DBC no se depende de carga automática de colecciones estilo JPA. Se consultan los identificadores relacionados y se arman explícitamente los DTO de respuesta.

Por ejemplo, para consultar una franquicia con sucursales, obtendré la franquicia y sus sucursales por `id_franquicia`. Para consultar una sucursal con productos, obtendré la sucursal y sus productos por `id_sucursal`. Evitaré ejecutar una consulta adicional por cada registro cuando pueda traer el conjunto en una sola consulta.

Las colecciones vacías se devolverán como `[]`, no como `null`.

## 4. Endpoints REST

Todos los endpoints utilizarán el prefijo `/api/v1`. Además de los criterios obligatorios, agregaré las consultas necesarias para recorrer las relaciones entre franquicias, sucursales y productos.

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/franquicias` | Consultar todas las franquicias |
| GET | `/sucursales` | Consultar todas las sucursales de todas las franquicias, incluyendo `idFranquicia` |
| GET | `/productos` | Consultar todos los productos de todas las sucursales, incluyendo `idSucursal` |
| GET | `/franquicias/{idFranquicia}` | Consultar una franquicia con todas sus sucursales |
| GET | `/franquicias/{idFranquicia}/sucursales` | Consultar todas las sucursales de la franquicia |
| GET | `/sucursales/{idSucursal}` | Consultar una sucursal con todos sus productos |
| GET | `/sucursales/{idSucursal}/productos` | Consultar todos los productos de la sucursal |
| GET | `/sucursales/{idSucursal}/productos/{idProducto}` | Consultar un producto de esa sucursal |
| POST | `/franquicias` | Crear una franquicia |
| POST | `/franquicias/{idFranquicia}/sucursales` | Crear una sucursal en la franquicia |
| POST | `/sucursales/{idSucursal}/productos` | Agregar un producto a la sucursal |
| DELETE | `/sucursales/{idSucursal}/productos/{idProducto}` | Eliminar un producto de la sucursal |
| PATCH | `/sucursales/{idSucursal}/productos/{idProducto}/stock` | Actualizar el stock de un producto |
| GET | `/franquicias/{idFranquicia}/productos/mayor-stock` | Consultar el producto de mayor stock por cada sucursal de la franquicia |
| PATCH | `/franquicias/{idFranquicia}` | Actualizar el nombre de una franquicia |
| PATCH | `/sucursales/{idSucursal}` | Actualizar el nombre de una sucursal |
| PATCH | `/sucursales/{idSucursal}/productos/{idProducto}` | Actualizar el nombre de un producto |

### Consultar una franquicia sin indicar sucursal

~~~http
GET /api/v1/franquicias/1
~~~

Debe traer la franquicia con la lista de **todas las sucursales que le pertenecen**:

~~~json
{
  "idFranquicia": 1,
  "nombre": "Franquicia Central",
  "sucursales": [
    {
      "idSucursal": 10,
      "nombre": "Sucursal Norte"
    },
    {
      "idSucursal": 11,
      "nombre": "Sucursal Sur"
    }
  ]
}
~~~

Si quiero únicamente las sucursales, sin los demás datos de la franquicia, usaré:

~~~http
GET /api/v1/franquicias/1/sucursales
~~~

Esta consulta devolverá una lista. Si la franquicia existe, pero no tiene sucursales, devolverá `[]`. Si la franquicia no existe, responderá `404`.

Las consultas globales, disponibles también desde Swagger, son:
- `GET /api/v1/franquicias`: todas las franquicias.
- `GET /api/v1/sucursales`: todas las sucursales, indicando su `idFranquicia`.
- `GET /api/v1/productos`: todos los productos, indicando su `idSucursal`.

Las tres devuelven `200 OK` y `data: []` si no existen registros, ordenadas de manera ascendente por identificador. La colección global no sustituye las consultas filtradas existentes.

### Consultar una sucursal sin indicar producto

~~~http
GET /api/v1/sucursales/10
~~~

Debe traer la sucursal con la lista de **todos los productos que tiene registrados**:

~~~json
{
  "idSucursal": 10,
  "idFranquicia": 1,
  "nombre": "Sucursal Norte",
  "productos": [
    {
      "idProducto": 100,
      "nombre": "Agua",
      "stock": 25
    },
    {
      "idProducto": 101,
      "nombre": "Gaseosa",
      "stock": 40
    }
  ]
}
~~~

Si quiero únicamente los productos de esa sucursal, usaré:

~~~http
GET /api/v1/sucursales/10/productos
~~~

Si la sucursal existe pero todavía no tiene productos, devolverá `[]`. Si no existe, responderá `404`.

Si indico un `idProducto`, devolverá únicamente ese producto y verificará que pertenezca a la sucursal indicada. Nunca debe consultar productos de otra sucursal por accidente.

### Crear registros

Crear una franquicia:

~~~http
POST /api/v1/franquicias
Content-Type: application/json

{
  "nombre": "Franquicia Central"
}
~~~

Agregar una sucursal:

~~~http
POST /api/v1/franquicias/1/sucursales
Content-Type: application/json

{
  "nombre": "Sucursal Norte"
}
~~~

Agregar un producto:

~~~http
POST /api/v1/sucursales/10/productos
Content-Type: application/json

{
  "nombre": "Agua",
  "stock": 25
}
~~~

Modificar el stock:

~~~http
PATCH /api/v1/sucursales/10/productos/100/stock
Content-Type: application/json

{
  "stock": 40
}
~~~

### Producto de mayor stock por cada sucursal

~~~http
GET /api/v1/franquicias/1/productos/mayor-stock
~~~

Ejemplo de respuesta:

~~~json
[
  {
    "idSucursal": 10,
    "nombreSucursal": "Sucursal Norte",
    "idProducto": 101,
    "nombreProducto": "Gaseosa",
    "stock": 40
  },
  {
    "idSucursal": 11,
    "nombreSucursal": "Sucursal Sur",
    "idProducto": 105,
    "nombreProducto": "Agua",
    "stock": 75
  }
]
~~~

La consulta buscará el mayor stock **dentro de cada sucursal** de la franquicia, no un único máximo para toda la franquicia. Si hay empate, seleccionará el producto con menor identificador. Las sucursales que no tengan productos no aparecerán en ese resultado.

En PostgreSQL puedo resolverlo mediante una función de ventana, evitando consultas independientes por cada sucursal:

~~~sql
SELECT
    resultado.id_sucursal,
    resultado.nombre_sucursal,
    resultado.id_producto,
    resultado.nombre_producto,
    resultado.stock
FROM (
    SELECT
        s.id_sucursal,
        s.nombre AS nombre_sucursal,
        p.id_producto,
        p.nombre AS nombre_producto,
        p.stock,
        ROW_NUMBER() OVER (
            PARTITION BY s.id_sucursal
            ORDER BY p.stock DESC, p.id_producto ASC
        ) AS posicion
    FROM sucursales s
    INNER JOIN productos p
        ON p.id_sucursal = s.id_sucursal
    WHERE s.id_franquicia = :idFranquicia
) resultado
WHERE resultado.posicion = 1
ORDER BY resultado.id_sucursal;
~~~

Antes de ejecutar esta consulta verificaré que la franquicia exista, para diferenciar una franquicia inexistente de una que todavía no tiene productos.

## 5. Validaciones y manejo de errores

Se validan los datos de entrada y las relaciones entre registros:

- No aceptar nombres vacíos ni nombres de más de 120 caracteres.
- No crear sucursales cuando la franquicia no exista.
- No crear productos cuando la sucursal no exista.
- No aceptar stock negativo.
- No permitir nombres duplicados dentro del mismo nivel, según las restricciones definidas en la base de datos.
- Comprobar que el producto pertenece a la sucursal indicada antes de consultarlo, modificarlo o eliminarlo.
- No mezclar las sucursales de diferentes franquicias ni los productos de diferentes sucursales.
- Retornar colecciones vacías si el registro padre existe y no tiene registros relacionados.
- Manejar actualizaciones concurrentes de stock sin sobrescribir silenciosamente un cambio que ya fue realizado.
- Mantener las claves foráneas y las restricciones de la base de datos como última protección de integridad.

Los códigos principales serán `201` para creaciones, `200` para consultas y actualizaciones, `204` para eliminaciones, `400` para datos inválidos, `404` cuando un recurso no exista y `409` cuando se presente una duplicidad o un conflicto de actualización.

Para mantener consistencia, centralizaré el manejo de errores y devolveré respuestas con información suficiente para identificar qué falló, sin exponer credenciales ni datos internos del servidor.

## 6. Organización del código

Organicé la aplicación en módulos de franquicias, sucursales y productos, separando los controladores, las interfaces de servicios, los casos de uso y los proveedores de datos. También definí componentes comunes para los DTO, las respuestas y la persistencia.

~~~text
src/main/java/com/moreno/gestorfranquicias/
├── modules/
│   ├── franquicias/
│   │   ├── api/
│   │   ├── usecase/
│   │   └── dataproviders/r2dbc/
│   ├── sucursales/
│   │   ├── api/
│   │   ├── usecase/
│   │   └── dataproviders/r2dbc/
│   └── productos/
│       ├── api/
│       ├── usecase/
│       └── dataproviders/r2dbc/
├── crosscutting/
│   ├── domain/
│   │   ├── common/
│   │   ├── request/
│   │   └── response/
│   └── persistence/
│       ├── franquicia/
│       ├── sucursal/
│       └── producto/
└── infrastructure/
    └── exception/
~~~

Los controladores solo reciben peticiones HTTP, validan el contrato de entrada y delegan la operación a un servicio. Los servicios contienen las reglas de negocio y dependen de contratos de proveedores, no directamente de los repositorios. Las implementaciones R2DBC se encargan de consultar PostgreSQL.

Los DTO de entrada y salida se mantienen separados de las entidades; los mapeadores centralizan la conversión. Utilicé nombres de métodos, variables y mensajes entendibles en español. Los registros de persistencia son records inmutables de Java para que las modificaciones sean explícitas.

Las decisiones están detalladas en [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md).

## 7. Flujo de trabajo con Git

El flujo habitual de integración utiliza pull requests hacia estas ramas permanentes. Las correcciones excepcionales de producción pueden aplicarse directamente a `main` y sincronizarse luego a los demás ambientes:

| Rama | Uso |
| --- | --- |
| `dev` | Desarrollo e integración de funcionalidades |
| `test` | Ejecución de pruebas e integración |
| `stage` | Validación de la versión candidata a producción |
| `main` | Código estable para producción |

Las ramas ya existen y se mantienen sincronizadas tras cada promoción o corrección urgente.

El flujo normal será:

~~~text
feature/* o bugfix/*
          |
          v
         dev
          |
          v
         test
          |
          v
        stage
          |
          v
         main
~~~

### Reglas para trabajar con ramas

1. Cada funcionalidad nueva se desarrollará en una rama `feature/*` creada desde la última versión de `dev`.
2. Las correcciones normales utilizarán `bugfix/*` desde `dev`.
3. Los cambios se integrarán mediante pull requests, respetando el orden `dev → test → stage → main`.
4. Antes de promover una versión se validarán las pruebas correspondientes.
5. Los cambios urgentes de producción utilizarán `hotfix/*` desde `main` y después se llevarán también a las ramas anteriores para mantenerlas sincronizadas.
6. No se realizará un despliegue de producción desde una rama de desarrollo.

GitHub Actions compila y ejecuta pruebas con cada etapa. El despliegue automático al VPS se ejecuta únicamente desde `main`, después de completar las validaciones.

## 8. Ejecución local

Hay dos formas independientes de ejecutar el proyecto: **todo con Docker Compose** o **API con Maven + PostgreSQL en Docker**. Utilice una sola modalidad para la API a la vez.

### Opción A — Docker Compose (recomendada para evaluar)

**Requisitos:** Git y Docker Engine con el plugin Docker Compose (o Docker Desktop en Windows/macOS), con el daemon en ejecución. No hace falta instalar Java ni Maven. Para construir las imágenes se necesita acceso a Internet la primera vez.

**1. Descargar el repositorio**

~~~bash
git clone https://github.com/mangelmorenomoreno/gestor-franquicias.git
cd gestor-franquicias
~~~

**2. Crear configuración local**

En Linux/macOS:

~~~bash
cp .env.example .env
~~~

En Windows PowerShell:

~~~powershell
Copy-Item .env.example .env
~~~

Abra `.env` y cambie `DB_PASSWORD` por una contraseña **local y privada** (conserve `DB_USERNAME=gestor_franquicias`). No suba este archivo a Git ni utilice credenciales del VPS. Esta contraseña se utiliza en los dos contenedores del Compose local.

**3. Construir e iniciar**

~~~bash
docker compose up -d --build
docker compose ps
~~~

`docker-compose.yml` construye la API desde el código fuente mediante el `Dockerfile` multietapa, inicia PostgreSQL 16 y espera a que esté saludable antes de iniciar la API. Flyway aplica las migraciones al arrancar Spring Boot. La primera compilación puede tardar por la descarga de dependencias.

**4. Verificar**

~~~bash
curl -fsS http://localhost:8088/actuator/health
curl -fsS http://localhost:8088/api/v1/franquicias
curl -fsS http://localhost:8088/v3/api-docs
~~~

El primer comando debe devolver `{"status":"UP"}`. La lista de franquicias puede estar vacía en una base nueva; la consulta debe responder satisfactoriamente. Abra `http://localhost:8088/swagger-ui/index.html` para probar el resto de operaciones desde el navegador. También puede usar `http://localhost:8088/swagger-ui.html`.

Puertos **solo de la máquina local** (enlazados a `127.0.0.1` en el Compose local):

| Servicio | Dirección local | Puerto interno |
| --- | --- | --- |
| API REST y Swagger | `http://localhost:8088` | 8080 |
| PostgreSQL | `localhost:5434` | 5432 |

**5. Logs y apagado**

~~~bash
docker compose logs -f api
# En otra terminal, para detener:
docker compose down
~~~

`docker compose down` **conserva** el volumen PostgreSQL. `docker compose down -v` lo elimina y **borra los datos locales**; usarlo exclusivamente cuando se desee reinicializar la base de pruebas.

### Opción B — Java 21 + Maven, con PostgreSQL en Docker

**Requisitos:** Java JDK 21, Maven compatible con Java 21, Git y Docker. Si utiliza esta opción, detenga previamente la API del Compose completo con `docker compose stop api`.

~~~bash
docker compose up -d base-datos
~~~

Abra `.env` y utilice exactamente el mismo usuario y contraseña en la aplicación Java. En Linux/macOS:

~~~bash
export DB_URL="r2dbc:postgresql://localhost:5434/gestor_franquicias"
export DB_MIGRATION_URL="jdbc:postgresql://localhost:5434/gestor_franquicias"
export DB_USERNAME="gestor_franquicias"
export DB_PASSWORD="CLAVE_ESTABLECIDA_EN_ENV"
mvn spring-boot:run
~~~

En Windows PowerShell:

~~~powershell
$env:DB_URL = "r2dbc:postgresql://localhost:5434/gestor_franquicias"
$env:DB_MIGRATION_URL = "jdbc:postgresql://localhost:5434/gestor_franquicias"
$env:DB_USERNAME = "gestor_franquicias"
$env:DB_PASSWORD = "CLAVE_ESTABLECIDA_EN_ENV"
mvn spring-boot:run
~~~

Sustituya `CLAVE_ESTABLECIDA_EN_ENV` por el valor de `DB_PASSWORD` de su archivo local. Spring Boot utiliza el perfil `dev` por defecto y escucha en `http://localhost:8080` cuando arranca mediante Maven (la publicación en `8088` corresponde a Docker Compose). La documentación estará en `http://localhost:8080/swagger-ui/index.html`.

### Ejecución de pruebas y revisión de calidad

Con Docker en ejecución (Testcontainers utiliza una base PostgreSQL temporal):

~~~bash
mvn -B clean verify
~~~

Incluye pruebas del backend y validación de migraciones según las suites del proyecto. GitHub Actions ejecuta además una construcción de imagen, comprueba los perfiles Spring `dev/test/stage/prod` con PostgreSQL y valida la configuración de Terraform. Los tests unitarios e integración no deben usar la base de producción.

**Prueba de escritura manual (solo en local o en datos de demostración):**

~~~bash
curl -i -X POST http://localhost:8088/api/v1/franquicias \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Franquicia de prueba local"}'
~~~

En PowerShell:

~~~powershell
Invoke-RestMethod -Uri "http://localhost:8088/api/v1/franquicias" -Method Post -ContentType "application/json" -Body '{"nombre":"Franquicia de prueba local"}'
~~~

Después, ejecute `GET /api/v1/franquicias` en Swagger o por `curl` y compruebe que la franquicia aparezca. La API utiliza un envoltorio estándar de respuesta que contiene `code`, `message`, `success`, `data`, `errors` y `timestamp`; los ejemplos de entidades de la sección 4 representan el contenido lógico de `data`.

### Problemas comunes

- **Docker no responde:** iniciar Docker Desktop / el servicio Docker y comprobar `docker version`.
- **Puertos ocupados:** el Compose local reserva `8088` para API y `5434` para PostgreSQL. Detenga el proceso que los utiliza o ajuste los puertos de `docker-compose.yml`; si cambia `5434`, ajuste también las URLs JDBC/R2DBC al ejecutar con Maven.
- **La base no autentica:** asegúrese de que las variables de la API y del contenedor coincidan. Cambiar `DB_PASSWORD` después de crear el volumen **no cambia la contraseña del usuario PostgreSQL existente**; gestione la contraseña desde PostgreSQL o restablezca solo la base local si puede perder esos datos.
- **La API no arranca:** revise `docker compose logs --tail=100 api` y `docker compose logs --tail=100 base-datos`.
- **Testcontainers falla:** confirme que Docker esté en ejecución y sea accesible para el usuario que lanza Maven.
- **Swagger no abre:** compruebe primero `/actuator/health`, luego `/v3/api-docs` y finalmente `/swagger-ui/index.html`.

## 9. Despliegue en la nube

El ambiente `prod` está desplegado en un VPS Linux y publica la API en `http://2.25.123.13:8088`. La base PostgreSQL y su volumen persisten en la red privada Docker; Terraform administra la red, el contenedor de PostgreSQL y el volumen del proyecto. El empaquetado en producción usa `Dockerfile.runtime` con el JAR generado y validado previamente en CI.

Al integrar cambios en `main`, el flujo normal es: **validar Maven/Testcontainers y Terraform → publicar imagen en GHCR etiquetada por SHA → desplegar solo producción por SSH → comprobar salud interna → probar desde Internet salud, OpenAPI, Swagger y lectura de franquicias**. El último despliegue funcional verificado para esta entrega es [GitHub Actions #44859340375](https://github.com/mangelmorenomoreno/gestor-franquicias/actions/runs/44859340375), commit `33001291d0e031838af99171d5882cd793c29d87`. Para revisiones posteriores consultar la ejecución del commit correspondiente.

La configuración de los secretos, las comprobaciones de despliegue, el aislamiento de perfiles y el procedimiento de recuperación se explican en [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md).

**Seguridad de la demostración:** el puerto `8088` de producción está expuesto públicamente en HTTP y actualmente la prueba no exige autenticación en los endpoints de escritura. Por ello, utilizar exclusivamente datos ficticios y no registrar información personal ni sensible. Para utilización real incorporar HTTPS, autenticación/autorización y límites de peticiones.

## 10. Estrategia de pruebas

Las pruebas implementadas y las validaciones de CI se enfocan en reglas del dominio, validación de nombres, integridad referencial, stock no negativo, concurrencia mediante versión, relaciones entre franquicias/sucursales/productos, consulta de máximo stock por sucursal, migraciones Flyway y persistencia con PostgreSQL/Testcontainers.

Para comprobar manualmente criterios de aceptación, crear por Swagger una franquicia, asociarle dos sucursales, crear productos con diferentes cantidades, actualizar un stock, consultar el máximo por sucursal, renombrar cada tipo de entidad y eliminar un producto. Repetir la consulta para verificar que desapareció. Usar solamente una base de prueba (preferiblemente local).

El flujo CI comprueba adicionalmente Docker Compose, Terraform y las rutas públicas de solo lectura una vez desplegado el perfil `prod`. El resultado visible por commit queda en la pestaña [Actions](https://github.com/mangelmorenomoreno/gestor-franquicias/actions).

## 11. Estado de la entrega

- [x] Endpoints obligatorios: franquicias, sucursales, productos, eliminación, actualización de stock y máximo por sucursal.
- [x] Funcionalidades opcionales: renombrado, API reactiva, Docker, Terraform y despliegue en nube.
- [x] Validaciones, migraciones, estructura modular y pruebas automatizadas.
- [x] Flujo de integración `dev → test → stage → main` y publicación GHCR.
- [x] Despliegue de producción verificado y endpoints públicos de solo lectura probados desde GitHub Actions.
- [x] Instrucciones de ejecución local con Docker y Maven.

**Evidencia del despliegue:** [workflow #44859340375](https://github.com/mangelmorenomoreno/gestor-franquicias/actions/runs/44859340375), SHA `33001291d0e031838af99171d5882cd793c29d87`. Esta evidencia corresponde exactamente a ese commit; un cambio posterior en documentación u otros archivos debe validarse con su propio flujo.

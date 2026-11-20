# Gestor de Franquicias

API REST para administrar franquicias, sucursales y productos. La estoy desarrollando como parte de una prueba técnica de backend, utilizando Java, Spring Boot y PostgreSQL.

La idea es manejar la información de cada franquicia, sus sucursales y el stock de los productos de forma independiente. También se podrá consultar qué producto tiene la mayor cantidad disponible en cada sucursal de una franquicia.

> **Estado del proyecto:** configuración inicial. Por ahora el repositorio contiene la documentación base; los endpoints, pruebas, contenedores y despliegue se irán incorporando en las siguientes etapas. Los ejemplos de este README muestran el contrato que voy a implementar, no servicios que ya estén publicados.

## 1. Funcionalidades

### Requisitos de la prueba

- Crear una franquicia.
- Agregar sucursales a una franquicia existente.
- Agregar productos a una sucursal.
- Eliminar un producto de una sucursal.
- Modificar la cantidad de stock de un producto.
- Consultar el producto con mayor stock de cada sucursal de una franquicia, indicando a qué sucursal pertenece.
- Guardar la información en PostgreSQL alojado en un proveedor de nube.

### Funcionalidades adicionales

También voy a incluir los puntos extra planteados en la prueba:

- Cambiar el nombre de una franquicia.
- Cambiar el nombre de una sucursal.
- Cambiar el nombre de un producto.
- Empaquetar la aplicación con Docker.
- Trabajar con programación reactiva utilizando Spring WebFlux y R2DBC.
- Definir la infraestructura necesaria con Terraform.
- Desplegar la API y PostgreSQL en la nube.

Además voy a incluir validaciones, manejo de errores, migraciones de base de datos, pruebas automatizadas y documentación OpenAPI/Swagger.

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

Voy a utilizar R2DBC para el acceso reactivo a PostgreSQL. Flyway manejará las migraciones de manera independiente usando una conexión JDBC para ese proceso.

## 3. Modelo de datos

La relación es sencilla: una franquicia puede tener varias sucursales, y cada sucursal puede tener varios productos.

~~~text
Franquicia (1)
    |
    +---- Sucursal (N)
               |
               +---- Producto (N)
~~~

**Franquicia**

- `idFranquicia`: identificador.
- `nombre`: nombre de la franquicia.

**Sucursal**

- `idSucursal`: identificador.
- `idFranquicia`: franquicia a la que pertenece.
- `nombre`: nombre de la sucursal.

**Producto**

- `idProducto`: identificador.
- `idSucursal`: sucursal a la que pertenece.
- `nombre`: nombre del producto.
- `stock`: cantidad disponible en esa sucursal.

El stock se maneja por sucursal. Si el mismo producto se vende en dos sucursales, cada registro tendrá su propia cantidad. Para el alcance de esta prueba no necesito crear un catálogo global de productos.

En PostgreSQL utilizaré nombres de tablas y columnas en `snake_case`; en Java, variables y métodos en `camelCase` con nombres que indiquen claramente su función.

## 4. Endpoints previstos

Todos los endpoints utilizarán el prefijo `/api/v1`.

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | `/franquicias` | Crear una franquicia |
| POST | `/franquicias/{idFranquicia}/sucursales` | Crear una sucursal |
| POST | `/sucursales/{idSucursal}/productos` | Agregar un producto |
| DELETE | `/sucursales/{idSucursal}/productos/{idProducto}` | Eliminar un producto |
| PATCH | `/sucursales/{idSucursal}/productos/{idProducto}/stock` | Actualizar el stock |
| GET | `/franquicias/{idFranquicia}/productos/mayor-stock` | Consultar el producto de mayor stock por sucursal |
| PATCH | `/franquicias/{idFranquicia}` | Actualizar el nombre de una franquicia |
| PATCH | `/sucursales/{idSucursal}` | Actualizar el nombre de una sucursal |
| PATCH | `/sucursales/{idSucursal}/productos/{idProducto}` | Actualizar el nombre de un producto |

### Ejemplos del contrato REST

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
POST /api/v1/sucursales/1/productos
Content-Type: application/json

{
  "nombre": "Producto A",
  "stock": 25
}
~~~

Modificar el stock:

~~~http
PATCH /api/v1/sucursales/1/productos/1/stock
Content-Type: application/json

{
  "stock": 40
}
~~~

Consultar el producto con mayor stock por sucursal:

~~~http
GET /api/v1/franquicias/1/productos/mayor-stock
~~~

Ejemplo de respuesta prevista:

~~~json
[
  {
    "idSucursal": 1,
    "nombreSucursal": "Sucursal Norte",
    "idProducto": 3,
    "nombreProducto": "Producto A",
    "stock": 40
  },
  {
    "idSucursal": 2,
    "nombreSucursal": "Sucursal Sur",
    "idProducto": 8,
    "nombreProducto": "Producto B",
    "stock": 75
  }
]
~~~

La consulta debe buscar el máximo dentro de **cada sucursal** de la franquicia solicitada, no un único máximo para toda la franquicia.

Si dos productos tienen el mismo stock máximo, voy a retornar el que tenga el identificador más bajo para que el resultado sea consistente. Las sucursales sin productos no aparecerán en ese listado.

## 5. Validaciones y manejo de errores

Voy a tener en cuenta estas reglas:

- El nombre de una franquicia, sucursal o producto no puede estar vacío.
- No se puede crear una sucursal si la franquicia no existe.
- No se puede crear un producto si la sucursal no existe.
- El stock debe ser un número entero mayor o igual a cero.
- Para modificar o eliminar un producto se debe validar que pertenezca a la sucursal indicada.
- Los errores se responderán con mensajes claros y códigos HTTP adecuados.

Los códigos principales serán `201` para creaciones, `200` para consultas y actualizaciones, `204` para eliminaciones, `400` para datos inválidos, `404` cuando el recurso no exista y `409` para conflictos cuando aplique.

## 6. Organización del código

Voy a separar las responsabilidades para que la lógica de negocio no quede dentro de los controladores.

Estructura propuesta:

~~~text
src/
├── main/
│   ├── java/
│   │   └── .../gestorfranquicias/
│   │       ├── franquicia/
│   │       ├── sucursal/
│   │       ├── producto/
│   │       └── compartido/
│   └── resources/
│       ├── application.yml
│       └── db/migration/
└── test/
~~~

Cada módulo tendrá sus controladores, servicios, repositorios, modelos y DTO según lo necesite. Las validaciones y las excepciones comunes quedarán centralizadas.

Voy a mantener nombres de clases, métodos, variables y mensajes entendibles, sin abreviaciones innecesarias. Los comentarios se agregarán cuando expliquen una decisión o una regla de negocio que no sea evidente.

## 7. Flujo de trabajo con Git

No voy a desarrollar directamente sobre `main`. Después de dejar lista la documentación inicial, voy a manejar estas ramas permanentes:

| Rama | Uso |
| --- | --- |
| `dev` | Desarrollo e integración de funcionalidades |
| `test` | Ejecución de pruebas e integración |
| `stage` | Validación de la versión candidata a producción |
| `main` | Código estable para producción |

Las ramas se crearán inicialmente en este orden: `stage` desde `main`, `test` desde `stage` y `dev` desde `test`.

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

GitHub Actions se configurará para compilar y ejecutar las pruebas de acuerdo con cada etapa. El despliegue al VPS se realizará desde `main`, una vez superadas las validaciones.

## 8. Ejecución local

**Pendiente de habilitar:** en este momento todavía no existe el código de la aplicación ni el archivo Docker Compose. Cuando estén disponibles, esta sección tendrá los comandos reales para levantar el entorno sin ajustes manuales.

La configuración local prevista es:

- Java 21 y Maven, si se ejecuta sin contenedores.
- Docker y Docker Compose para ejecutar API y PostgreSQL.
- Variables de entorno para la URL de la base de datos, usuario y contraseña.
- Flyway para aplicar las migraciones al iniciar.

Variables previstas:

~~~dotenv
DB_URL=r2dbc:postgresql://localhost:5432/gestor_franquicias
DB_USERNAME=gestor_franquicias
DB_PASSWORD=CAMBIAR_POR_CLAVE_LOCAL

# Solo para las migraciones con Flyway
DB_MIGRATION_URL=jdbc:postgresql://localhost:5432/gestor_franquicias
~~~

Estos valores son de ejemplo, no credenciales reales. La configuración definitiva quedará en `.env.example` y las contraseñas no se subirán al repositorio.

Cuando el proyecto esté implementado, documentaré aquí los comandos exactos para compilar, iniciar la aplicación, ejecutar las pruebas y abrir Swagger.

## 9. Despliegue en la nube

Para la prueba voy a utilizar un VPS Linux con Docker. La API y PostgreSQL estarán en recursos independientes del resto de aplicaciones del servidor.

La ruta prevista es:

~~~text
/opt/gestor-franquicias/
~~~

La base de datos tendrá su volumen persistente y no expondrá su puerto a Internet. La API se publicará en un puerto específico del servidor; el acceso inicial podrá hacerse mediante la IP pública, sin necesidad de un dominio.

Terraform se utilizará para describir y reproducir los recursos de esta aplicación que sean apropiados para administrar como código. No se modificarán los contenedores ni los volúmenes de otras aplicaciones.

El despliegue estará pendiente hasta completar las pruebas. En ese momento agregaré aquí la URL real, el procedimiento y las indicaciones para verificar los endpoints.

## 10. Pruebas

Voy a cubrir tanto los casos correctos como los casos de error:

- Creación de franquicias, sucursales y productos.
- Validación de nombres vacíos y stock negativo.
- Intentos de modificar o eliminar productos que no pertenezcan a una sucursal.
- Consulta de mayor stock con varias sucursales.
- Empates en la cantidad de stock.
- Sucursales sin productos.
- Actualización de los nombres.
- Persistencia de la información en PostgreSQL.

Utilizaré pruebas unitarias para la lógica de negocio y pruebas de integración para validar el comportamiento real de las consultas y la API.

## 11. Avance del proyecto

- [x] Crear el repositorio público.
- [x] Definir el alcance y el diseño inicial.
- [ ] Crear las ramas `dev`, `test` y `stage`.
- [ ] Inicializar Spring Boot y configurar PostgreSQL.
- [ ] Implementar los endpoints obligatorios.
- [ ] Implementar los endpoints adicionales.
- [ ] Agregar pruebas unitarias y de integración.
- [ ] Configurar Docker, Compose y Flyway.
- [ ] Configurar Terraform y GitHub Actions.
- [ ] Desplegar la solución en la nube y verificarla.
- [ ] Completar los comandos y la URL de ejecución.

Iré actualizando este README junto con los cambios del proyecto para que la documentación siempre corresponda con el código que está en el repositorio.

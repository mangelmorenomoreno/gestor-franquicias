# Análisis de la prueba técnica

## Descripción general

La prueba solicita una API para administrar **franquicias, sucursales y productos**. Una franquicia tiene varias sucursales; cada sucursal tiene sus propios productos y cada producto registra un nombre y una cantidad de stock.

La solución utiliza **Java 21, Spring Boot 3.5, WebFlux, R2DBC y PostgreSQL 16**. La implementación está organizada por funcionalidades y separa los controladores, los casos de uso y el acceso a datos.

Este documento relaciona los criterios del ejercicio con las operaciones desarrolladas y los archivos donde se encuentran. Las rutas pueden consultarse también en [Swagger](http://2.25.123.13:8088/swagger-ui/index.html) y en los [ejemplos de la API](EJEMPLOS_API.md).

## 1. Criterios de aceptación

### 1.1. Desarrollar el proyecto en Spring Boot

Se creó una aplicación con **Spring Boot 3.5.8 y Java 21**. Los servicios HTTP se implementaron con Spring WebFlux; la validación de solicitudes se realiza con Jakarta Validation.

La aplicación inicia desde [GestorFranquiciasApplication.java](../src/main/java/com/moreno/gestorfranquicias/GestorFranquiciasApplication.java) y sus dependencias están declaradas en [pom.xml](../pom.xml). La configuración de conexión a la base de datos está en [application.yml](../src/main/resources/application.yml).

### 1.2. Agregar una nueva franquicia

Se implementó el endpoint:

\`\`\`http
POST /api/v1/franquicias
Content-Type: application/json

{"nombre": "Franquicia Central"}
\`\`\`

El controlador recibe el nombre, el servicio elimina espacios al inicio y al final y el proveedor de datos guarda el registro. Si la creación es correcta, la API responde con **201 Created** y devuelve el identificador y el nombre de la franquicia.

**Código:** [FranquiciaController.java](../src/main/java/com/moreno/gestorfranquicias/modules/franquicias/api/FranquiciaController.java) y [FranquiciaService.java](../src/main/java/com/moreno/gestorfranquicias/modules/franquicias/usecase/FranquiciaService.java).

### 1.3. Agregar una sucursal a una franquicia

Se implementó el endpoint:

\`\`\`http
POST /api/v1/franquicias/{idFranquicia}/sucursales
Content-Type: application/json

{"nombre": "Sucursal Norte"}
\`\`\`

Antes de registrar la sucursal se comprueba que la franquicia exista. La relación se almacena mediante la clave foránea \`id_franquicia\`, por lo que cada sucursal queda asociada a una franquicia concreta.

**Código:** [SucursalController.java](../src/main/java/com/moreno/gestorfranquicias/modules/sucursales/api/SucursalController.java) y [SucursalService.java](../src/main/java/com/moreno/gestorfranquicias/modules/sucursales/usecase/SucursalService.java).

### 1.4. Agregar un producto a una sucursal

Se implementó el endpoint:

\`\`\`http
POST /api/v1/sucursales/{idSucursal}/productos
Content-Type: application/json

{"nombre": "Café", "stock": 20}
\`\`\`

El servicio verifica que la sucursal exista y guarda el producto con su identificador de sucursal. Se exige un nombre válido y una cantidad de stock **mayor o igual que cero**. El mismo nombre de producto puede utilizarse en sucursales distintas.

**Código:** [ProductoController.java](../src/main/java/com/moreno/gestorfranquicias/modules/productos/api/ProductoController.java) y [ProductoService.java](../src/main/java/com/moreno/gestorfranquicias/modules/productos/usecase/ProductoService.java).

### 1.5. Eliminar un producto de una sucursal

Se implementó el endpoint:

\`\`\`http
DELETE /api/v1/sucursales/{idSucursal}/productos/{idProducto}
\`\`\`

Antes de eliminar el registro se valida que el producto pertenezca a la sucursal indicada. Si la operación termina correctamente, la respuesta es **204 No Content**, sin cuerpo. No se añadieron operaciones para eliminar franquicias o sucursales porque no forman parte de los criterios solicitados.

**Código:** método \`eliminar\` de [ProductoService.java](../src/main/java/com/moreno/gestorfranquicias/modules/productos/usecase/ProductoService.java) y su endpoint en [ProductoController.java](../src/main/java/com/moreno/gestorfranquicias/modules/productos/api/ProductoController.java).

### 1.6. Modificar el stock de un producto

Se implementó el endpoint:

\`\`\`http
PATCH /api/v1/sucursales/{idSucursal}/productos/{idProducto}/stock
Content-Type: application/json

{"stock": 30}
\`\`\`

La API consulta el producto, comprueba que corresponda a la sucursal y actualiza el valor. Se valida que la cantidad no sea negativa.

La tabla de productos tiene un campo \`version\` para detectar actualizaciones concurrentes sobre el mismo registro. Esto evita sobrescribir silenciosamente una modificación cuando otra operación ya cambió el producto.

**Código:** [ProductoService.java](../src/main/java/com/moreno/gestorfranquicias/modules/productos/usecase/ProductoService.java), [ProductoEntity.java](../src/main/java/com/moreno/gestorfranquicias/crosscutting/persistence/producto/entity/ProductoEntity.java) y la [migración de base de datos](../src/main/resources/db/migration/V1__crear_modelo.sql).

### 1.7. Consultar el producto con mayor stock por sucursal de una franquicia

Se implementó el endpoint:

\`\`\`http
GET /api/v1/franquicias/{idFranquicia}/productos/mayor-stock
\`\`\`

La consulta devuelve **un producto por cada sucursal que tenga productos**, identificando tanto la sucursal como el producto y su cantidad de stock. No se obtiene un único máximo para toda la franquicia.

Para resolverlo se utilizó \`ROW_NUMBER()\` en PostgreSQL, agrupando los resultados por sucursal y ordenándolos por stock descendente. Si dos productos tienen la misma cantidad, se selecciona el de menor identificador. Las sucursales sin productos no aparecen en esta consulta.

Ejemplo del contenido de \`data\`:

\`\`\`json
[
  {
    "idSucursal": 10,
    "nombreSucursal": "Sucursal Norte",
    "idProducto": 100,
    "nombreProducto": "Café",
    "stock": 30
  },
  {
    "idSucursal": 11,
    "nombreSucursal": "Sucursal Sur",
    "idProducto": 103,
    "nombreProducto": "Agua",
    "stock": 45
  }
]
\`\`\`

**Código:** [ProductoDataProvider.java](../src/main/java/com/moreno/gestorfranquicias/modules/productos/dataproviders/r2dbc/ProductoDataProvider.java) y [FranquiciaService.java](../src/main/java/com/moreno/gestorfranquicias/modules/franquicias/usecase/FranquiciaService.java).

### 1.8. Persistir la información en una base de datos

Se eligió **PostgreSQL 16** para mantener las relaciones entre franquicias, sucursales y productos mediante claves foráneas. Los datos no quedan almacenados en memoria: se guardan en PostgreSQL y se conservan en un volumen Docker.

El acceso a datos de la aplicación utiliza **R2DBC** y las migraciones de esquema se ejecutan con **Flyway**, mediante una conexión JDBC separada. La migración inicial crea las tres tablas, los índices, las restricciones de nombre y las reglas de stock.

En producción PostgreSQL se ejecuta en un **VPS**, dentro de un contenedor con volumen persistente. No se utiliza un servicio administrado de base de datos como RDS; la infraestructura de persistencia se administra en el propio servidor.

**Archivos:** [V1__crear_modelo.sql](../src/main/resources/db/migration/V1__crear_modelo.sql), [application.yml](../src/main/resources/application.yml) y [Terraform](../infra/terraform/main.tf).

## 2. Puntos extra

### 2.1. Empaquetar la aplicación con Docker

Se agregó un [Dockerfile](../Dockerfile) multietapa: primero compila el proyecto con Maven y Java 21; después ejecuta el JAR sobre una imagen JRE de Java 21. La aplicación se ejecuta con un usuario sin privilegios de administrador.

Para desarrollo, [docker-compose.yml](../docker-compose.yml) inicia la API y PostgreSQL mediante un único comando:

\`\`\`bash
docker compose up -d --build
\`\`\`

También se creó [Dockerfile.runtime](../Dockerfile.runtime) para construir la imagen de despliegue a partir del JAR generado durante la integración continua.

### 2.2. Utilizar programación funcional o reactiva

Se implementó programación reactiva con **Spring WebFlux, Project Reactor y Spring Data R2DBC**.

Los métodos utilizan \`Mono<T>\` cuando entregan un único resultado, por ejemplo al crear o actualizar un producto, y \`Flux<T>\` cuando pueden devolver varios registros, como al listar sucursales o consultar los productos con mayor stock.

Se utilizan operadores como \`map\`, \`flatMap\`, \`thenMany\` y \`collectList\` para transformar los resultados, encadenar validaciones y construir las respuestas HTTP sin recurrir a consultas JPA bloqueantes.

**Archivos:** [FranquiciaService.java](../src/main/java/com/moreno/gestorfranquicias/modules/franquicias/usecase/FranquiciaService.java), [SucursalService.java](../src/main/java/com/moreno/gestorfranquicias/modules/sucursales/usecase/SucursalService.java) y [ProductoService.java](../src/main/java/com/moreno/gestorfranquicias/modules/productos/usecase/ProductoService.java).

### 2.3. Actualizar el nombre de una franquicia

\`\`\`http
PATCH /api/v1/franquicias/{idFranquicia}
Content-Type: application/json

{"nombre": "Franquicia Renovada"}
\`\`\`

Se busca la franquicia y se guarda el nombre actualizado. El cambio no afecta sus sucursales.

**Código:** [FranquiciaController.java](../src/main/java/com/moreno/gestorfranquicias/modules/franquicias/api/FranquiciaController.java).

### 2.4. Actualizar el nombre de una sucursal

\`\`\`http
PATCH /api/v1/sucursales/{idSucursal}
Content-Type: application/json

{"nombre": "Sucursal Parque"}
\`\`\`

El servicio cambia el nombre conservando la asociación con la franquicia.

**Código:** [SucursalController.java](../src/main/java/com/moreno/gestorfranquicias/modules/sucursales/api/SucursalController.java).

### 2.5. Actualizar el nombre de un producto

\`\`\`http
PATCH /api/v1/sucursales/{idSucursal}/productos/{idProducto}
Content-Type: application/json

{"nombre": "Café Premium"}
\`\`\`

Se valida la relación con la sucursal y se modifica únicamente el nombre, conservando el stock. La actualización también utiliza el control de versión del producto.

**Código:** [ProductoController.java](../src/main/java/com/moreno/gestorfranquicias/modules/productos/api/ProductoController.java).

### 2.6. Aprovisionar la persistencia con infraestructura como código (IaC)

Se eligió **Terraform** con el proveedor de Docker. La infraestructura está declarada en [infra/terraform/main.tf](../infra/terraform/main.tf) e incluye:

- Una red Docker privada para conectar la API con PostgreSQL.
- Un volumen para conservar los datos de PostgreSQL.
- La imagen y el contenedor de PostgreSQL 16, con variables de conexión y comprobación de salud.

Durante el despliegue, [desplegar-vps.sh](../scripts/desplegar-vps.sh) inicializa Terraform, revisa el plan y aplica los recursos necesarios. El script también comprueba que el plan no incluya una eliminación del volumen de datos.

**Alcance:** Terraform administra esos recursos **dentro de un VPS existente**. No crea la máquina virtual ni aprovisiona un servicio de base de datos administrado en un proveedor cloud.

### 2.7. Desplegar la solución en la nube

Se publicó la aplicación en un **VPS Linux**. La imagen de producción se almacena en **GitHub Container Registry (GHCR)** y el despliegue se realiza mediante GitHub Actions, SSH, Terraform y Docker Compose.

El proceso automático compila y prueba el proyecto, publica la imagen y actualiza la API del servidor desde la rama \`main\`. Después comprueba la salud de la aplicación, OpenAPI, Swagger y una consulta de franquicias.

La instancia disponible corresponde al perfil **\`prod\`**, en el puerto **8088**. Los perfiles \`dev\`, \`test\` y \`stage\` tienen configuración independiente y se validan en CI, pero **no están desplegados en el VPS**.

- [Swagger de producción](http://2.25.123.13:8088/swagger-ui/index.html)
- [Guía de despliegue y automatización](DESPLIEGUE.md)

## 3. Consideraciones de implementación

Además de las operaciones solicitadas, se incluyeron consultas de franquicias, sucursales y productos para recorrer las relaciones y revisar los registros desde Swagger.

La API utiliza el prefijo \`/api/v1\`, DTO para las solicitudes y respuestas, validaciones de nombres y stock, e índices que evitan nombres duplicados dentro del ámbito correspondiente. El manejo de errores centraliza respuestas \`400\`, \`404\` y \`409\` para solicitudes inválidas, recursos inexistentes y conflictos de datos o concurrencia.

La estructura del proyecto está organizada por módulos funcionales y separa la lógica de negocio del acceso a PostgreSQL. Para los contratos y ejemplos de respuesta de las **17 rutas**, consultar [EJEMPLOS_API.md](EJEMPLOS_API.md).

## 4. Pruebas y validación

Se implementaron pruebas unitarias con **JUnit, Mockito y Reactor Test**, además de pruebas de integración utilizando **Testcontainers con PostgreSQL**. Entre los escenarios cubiertos están la creación de relaciones, el inventario, la actualización de stock, la consulta de mayor stock, la eliminación de productos y las respuestas cuando una colección está vacía o un registro no existe.

La ejecución principal de pruebas utiliza:

\`\`\`bash
mvn clean verify
\`\`\`

En GitHub Actions se valida también la compilación, el empaquetado Docker, los perfiles de configuración, las migraciones de Flyway y Terraform. Otro workflow inicia el proyecto de las dos maneras documentadas: **Docker Compose completo** y **Maven con PostgreSQL en Docker**, y comprueba los endpoints HTTP.

Los scripts y cada etapa de integración y despliegue se explican en [DESPLIEGUE.md](DESPLIEGUE.md).

## 5. Repositorio e instrucciones de ejecución

El proyecto se encuentra en un [repositorio público de GitHub](https://github.com/mangelmorenomoreno/gestor-franquicias). Se trabajó con ramas de desarrollo, pruebas, preproducción y producción; la integración continua valida cambios antes de publicar una versión.

La ejecución local no requiere acceso al VPS ni configurar los secretos del servidor. Para iniciar todo con Docker, desde la raíz del proyecto:

\`\`\`bash
docker compose up -d --build
\`\`\`

Después puede consultarse [Swagger local](http://localhost:8088/swagger-ui/index.html). La configuración de demostración ya incluye las credenciales locales de PostgreSQL.

También se puede ejecutar la API mediante Maven, con PostgreSQL en Docker. Los comandos para Windows y Linux/macOS están en el [README principal](../README.md#ejecutar-localmente).

---

**Documentos relacionados:** [README principal](../README.md) · [Arquitectura](ARQUITECTURA.md) · [Ejemplos de API](EJEMPLOS_API.md) · [Despliegue](DESPLIEGUE.md) · [Terraform](../infra/terraform/README.md).

**Nota de uso:** la API pública es una demostración accesible por HTTP y no incluye autenticación para los endpoints de escritura. Debe utilizarse con datos de prueba, no con información sensible.

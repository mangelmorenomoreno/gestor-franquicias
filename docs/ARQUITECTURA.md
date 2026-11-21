# Arquitectura

El backend está organizado por funcionalidad: franquicias, sucursales y productos. Los controladores gestionan las solicitudes HTTP, los servicios aplican las reglas de negocio y los proveedores de datos implementan las consultas a PostgreSQL.

```text
HTTP / JSON
    |
Controlador (api)
    |
Servicio (usecase)
    |
Interfaz de proveedor de datos
    |
Implementación R2DBC
    |
Repositorio / PostgreSQL
```

Los DTO de entrada y salida están separados de las entidades de persistencia. Los mapeadores convierten las entidades en respuestas de la API.

## Programación reactiva

Spring WebFlux y R2DBC permiten mantener las operaciones de base de datos dentro del flujo reactivo. Los servicios utilizan:

- **`Mono<T>`** para buscar, guardar o actualizar un solo elemento; `Mono<Void>` para eliminar sin devolver contenido.
- **`Flux<T>`** para listar franquicias, sucursales, productos y resultados de consultas que pueden contener varios registros.
- **`flatMap`** cuando la siguiente operación depende del dato obtenido.
- **`then` / `thenMany`** cuando primero se debe validar una relación y después ejecutar otra consulta.
- **`collectList`** para construir respuestas que incluyen una colección dentro de un DTO.

Flyway utiliza una conexión JDBC para aplicar las migraciones al iniciar la aplicación. Las consultas de negocio se realizan mediante R2DBC.

## Modelo de datos

```text
Franquicia (1) ---- (N) Sucursal (1) ---- (N) Producto
```

Una sucursal pertenece a una franquicia y un producto pertenece a una sucursal. El stock se guarda en cada producto. Las claves foráneas conservan la integridad de las relaciones.

Los nombres tienen índices de unicidad en el ámbito correspondiente. Los productos utilizan un campo de versión para detectar actualizaciones concurrentes. La consulta de mayor stock usa `ROW_NUMBER()`, particionando por sucursal y ordenando por stock descendente e identificador ascendente; así se obtiene un resultado por sucursal sin realizar consultas independientes.

## Pruebas

Hay pruebas unitarias de los servicios y pruebas de integración con PostgreSQL mediante Testcontainers. Se validan relaciones, inventario, cambios de nombre, consulta de mayor stock y respuestas de error. El proyecto se compila y prueba con:

```bash
mvn clean verify
```

## Ejecución y despliegue

El [README principal](../README.md#ejecutar-localmente) incluye la ejecución local con Docker Compose y con Java 21/Maven. Docker Compose ofrece credenciales predeterminadas exclusivas para desarrollo, de forma que el proyecto se inicia sin crear `.env`. Los scripts de inicio Maven utilizan esos mismos valores o las variables definidas por el desarrollador.

Los contratos de las rutas y sus respuestas están en [Ejemplos de la API](EJEMPLOS_API.md). Los archivos que intervienen en la compilación, publicación de imágenes y despliegue se detallan en [Despliegue y GitHub Actions](DESPLIEGUE.md).

En el VPS solo se despliega el perfil `prod` desde `main`. Terraform prepara la red, el volumen y PostgreSQL; Docker Compose ejecuta la API. Los perfiles `dev`, `test` y `stage` se validan en CI, pero no están desplegados en el VPS.

La guía de configuración está en [Despliegue](DESPLIEGUE.md).

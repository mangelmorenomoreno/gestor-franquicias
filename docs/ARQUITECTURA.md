# Arquitectura de Gestor de Franquicias

Organicé el backend separando la recepción de solicitudes, las reglas del negocio y las consultas a la base de datos. Cada módulo mantiene sus propias interfaces y casos de uso, mientras que los contratos y componentes compartidos se encuentran en paquetes transversales.

## Flujo de una solicitud

~~~text
HTTP / JSON
   |
   v
modules/*/api (Controller)
   |
   v
modules/*/usecase (IService -> Service)
   |
   v
modules/*/dataproviders (IDataProvider -> implementación R2DBC)
   |
   v
crosscutting/persistence (Repository / Entity / Mapper)
   |
   v
PostgreSQL
~~~

Los controladores reciben la solicitud HTTP, validan la entrada y construyen la respuesta. Los casos de uso contienen las reglas funcionales y dependen de interfaces de proveedores de datos. Las implementaciones R2DBC resuelven las consultas y actualizaciones en PostgreSQL. Los DTO de entrada y salida se mantienen separados de las entidades.

## Decisiones técnicas

- Utilizo Spring WebFlux y Spring Data R2DBC para mantener reactivo el acceso a PostgreSQL.
- La respuesta estándar utiliza los campos code, message, success, data, errors y timestamp.
- Represento las entidades de persistencia mediante records de Java y utilizo un campo de versión en productos para controlar modificaciones concurrentes.
- La consulta de mayor stock utiliza ROW_NUMBER() en PostgreSQL, con un resultado por sucursal y desempate por identificador.
- Las validaciones se realizan en los DTO y también mediante restricciones e índices en PostgreSQL.
- Mantengo una estructura por funcionalidades sin incorporar servicios que no hacen parte de esta prueba.

## Relaciones

Una franquicia puede tener cero o muchas sucursales, y una sucursal puede tener cero o muchos productos. Cuando un registro existe pero no tiene registros asociados, la respuesta devuelve una lista vacía. Las modificaciones de productos validan su pertenencia a la sucursal indicada.

## Ramas y validaciones

Normalmente las ramas `feature/*` se crean desde `dev`. La promoción sigue `dev → test → stage → main` mediante pull requests. Las correcciones excepcionales de producción pueden aplicarse a `main`, y después deben propagarse a las demás ramas mediante avances no forzados.

El workflow de validación está configurado para ejecutarse con pushes a las ramas permanentes o manualmente. Incluí pruebas unitarias y una prueba de integración con PostgreSQL mediante Testcontainers.

## Infraestructura

Docker Compose permite iniciar la API y PostgreSQL localmente. Terraform define recursos propios del proyecto en el VPS: contenedor de PostgreSQL, volumen de datos y red Docker. **Solo el perfil `prod` de `main` fue desplegado en el VPS (`8088`)**. Los perfiles `dev` (`8086`), `test` (`8087`) y `stage` (`8089`) poseen puertos reservados en la configuración, pero se validan en CI y **no son ambientes publicados en el servidor**.

El estado de Terraform puede contener información sensible, por lo que debe mantenerse fuera de Git. El despliegue de producción ya se verificó en el VPS: [ejecución #44859340375](https://github.com/mangelmorenomoreno/gestor-franquicias/actions/runs/44859340375), imagen de commit `33001291d0e031838af99171d5882cd793c29d87`.

## Empaquetado y validación

Durante la integración continua, Maven compila y ejecuta las pruebas usando su caché de dependencias. El JAR verificado se copia a un directorio temporal `empaquetado/` que está excluido de Git.

El archivo `Dockerfile.runtime` construye la imagen utilizando ese JAR, sin descargar nuevamente dependencias de Maven. Esto reduce solicitudes innecesarias al repositorio de artefactos y evita que una limitación temporal del servidor de dependencias afecte a una imagen cuyo código ya fue validado.

El archivo `Dockerfile` se conserva para construir la aplicación directamente con Docker Compose desde el código fuente en un equipo de desarrollo.


## Ejecución, dependencias e interoperabilidad

El servicio HTTP usa **Spring WebFlux** y los repositorios acceden a PostgreSQL mediante **R2DBC**, utilizando composición reactiva en los casos de uso. **Flyway** aplica las migraciones utilizando su conexión JDBC, separada del acceso reactivo de negocio. Esto evita depender de JPA/Hibernate para las relaciones del modelo y conserva el control explícito de las consultas y DTO.

La aplicación se puede ejecutar desde el código fuente de dos formas:

- Con `docker compose up -d --build`: el Dockerfile multietapa compila con Maven/Java 21 y ejecuta Java 21 en un contenedor, junto con PostgreSQL 16; API en `localhost:8088`.
- Con `mvn spring-boot:run` y PostgreSQL levantado por Docker: API en `localhost:8080`; se configuran `DB_URL`, `DB_MIGRATION_URL`, `DB_USERNAME` y `DB_PASSWORD`.

Las instrucciones exactas y los comandos para Windows y Linux/macOS están en [README, sección Ejecución local](../README.md#8-ejecución-local).

## Evidencia automatizada y límites

La ejecución de GitHub Actions [#44859340375](https://github.com/mangelmorenomoreno/gestor-franquicias/actions/runs/44859340375) comprobó la publicación del JAR como imagen GHCR, Terraform, PostgreSQL, arranque del contenedor de producción, correspondencia con SHA de commit y acceso externo a las rutas de salud, OpenAPI, Swagger y lectura de franquicias.

Esto demuestra funcionamiento de esa revisión, **no** que toda operación o caso límite esté probado en producción. Los cambios funcionales deben validarse mediante las suites Maven y pruebas de integración en PostgreSQL/Testcontainers. El flujo evita realizar escrituras destructivas sobre la base pública.

## Alcance y decisiones conscientes

- La estructura por módulos y los contratos de acceso a datos **aplica separación de responsabilidades y principios de arquitectura limpia**, sin introducir microservicios ni complejidad adicional innecesaria para esta prueba.
- La consulta de máximo stock retorna el producto con mayor inventario por sucursal, incluyendo identificador y nombre de la sucursal; si hay empates, el criterio de desempate se establece por identificador.
- Terraform administra exclusivamente red, volumen y contenedor PostgreSQL del proyecto en el VPS; el contenedor API se despliega mediante Docker Compose en la misma red.
- Por petición de evaluación, la API está accesible en `http://2.25.123.13:8088` y Swagger en `/swagger-ui/index.html`, **sin HTTPS ni autenticación de escritura**. Esto no debe adoptarse como postura de seguridad para datos reales. Se recomienda un proxy HTTPS, controles de acceso y limitación de peticiones.

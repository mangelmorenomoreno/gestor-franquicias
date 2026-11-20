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

Las ramas feature/* se crean desde dev. La integración sigue dev, test, stage y main mediante pull requests. Los hotfix de producción salen de main y deben sincronizarse con las demás ramas.

El workflow de validación está configurado para ejecutarse con pushes a las ramas permanentes o manualmente. Incluí pruebas unitarias y una prueba de integración con PostgreSQL mediante Testcontainers.

## Infraestructura

Docker Compose permite iniciar la API y PostgreSQL localmente. Terraform define recursos propios del proyecto en el VPS: contenedor de PostgreSQL, volumen de datos y red Docker.

El estado de Terraform puede contener información sensible, por lo que debe mantenerse fuera de Git. El despliegue en el VPS está pendiente de ejecución y validación.

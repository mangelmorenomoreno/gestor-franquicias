# Infraestructura PostgreSQL con Terraform

Esta carpeta define los recursos Docker propios de la aplicación: red privada, volumen persistente y contenedor PostgreSQL 16. La API se ejecuta con Docker Compose y utiliza la red creada por Terraform para conectarse a la base de datos.

## Estado del despliegue

**Únicamente se ha desplegado producción (`prod`) en el VPS.** Los perfiles `dev`, `test` y `stage` cuentan con configuración independiente, pero solo se validan temporalmente en integración continua; no están instalados ni expuestos en el servidor como parte de esta entrega.

| Perfil | Estado real en el VPS | Red Docker | Volumen PostgreSQL | Contenedor PostgreSQL |
| --- | --- | --- | --- | --- |
| `dev` | No desplegado | `gestor-franquicias-dev-red` | `gestor-franquicias-dev-datos` | `gestor-franquicias-dev-postgres` |
| `test` | No desplegado | `gestor-franquicias-test-red` | `gestor-franquicias-test-datos` | `gestor-franquicias-test-postgres` |
| `stage` | No desplegado | `gestor-franquicias-stage-red` | `gestor-franquicias-stage-datos` | `gestor-franquicias-stage-postgres` |
| `prod` | Desplegado y verificado | `gestor-franquicias-prod-red` | `gestor-franquicias-prod-datos` | `gestor-franquicias-prod-postgres` |

Los nombres de las tres primeras filas son **identificadores configurados para posibles despliegues**, no prueba de que esos recursos existan en el VPS. Los recursos de producción se administran en `/opt/gestor-franquicias/prod/infra/terraform`, con estado Terraform independiente.

El despliegue de producción del commit `33001291d0e031838af99171d5882cd793c29d87` fue validado en [GitHub Actions, ejecución #44859340375](https://github.com/mangelmorenomoreno/gestor-franquicias/actions/runs/44859340375). Los commits posteriores requieren su propia validación.

## Archivos y variables

- `main.tf`: red, volumen e instancia de PostgreSQL mediante el proveedor `kreuzwerker/docker`.
- `versions.tf`: restricciones de versiones de Terraform y su proveedor.
- `variables.tf`: perfil, nombre de base, usuario y contraseña.
- `terraform.tfvars.example`: ejemplo de variables de conexión, sin credenciales válidas.

La variable `ambiente` acepta `dev`, `test`, `stage` o `prod`. `clave_base_datos` es sensible y debe proporcionarse por un canal seguro; no se almacena en Git. El flujo de producción recibe sus valores desde la configuración y los secretos de GitHub.

## Validar sin desplegar

Desde la raíz del repositorio, con Terraform instalado:

~~~bash
terraform -chdir=infra/terraform init -backend=false -input=false
terraform -chdir=infra/terraform validate -no-color
~~~

La aplicación puede ejecutarse localmente **sin Terraform**, utilizando `docker compose up -d --build`. Las instrucciones completas están en el [README principal](../../README.md#8-ejecución-local).

## Aislamiento y seguridad

El estado Terraform puede contener información sensible; se mantiene fuera del repositorio y requiere copias de seguridad con permisos restringidos. El script de despliegue inspecciona el plan y rechaza operaciones que impliquen eliminar el volumen de datos. No se ejecuta `terraform destroy` automáticamente.

Terraform administra recursos Docker **dentro de un VPS existente**. No aprovisiona la máquina virtual ni configura la red pública del proveedor. Para más información sobre CI/CD, SSH, GHCR y acceso externo, consultar [DESPLIEGUE.md](../../docs/DESPLIEGUE.md).

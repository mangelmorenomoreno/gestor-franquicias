# PostgreSQL aislado por entorno

Terraform utiliza el mismo código de infraestructura con la variable `ambiente` para `dev`, `test`, `stage` y `prod`.

Cada ambiente cuenta con un directorio independiente en el VPS (`/opt/gestor-franquicias/<ambiente>/infra/terraform`) y **su propio estado Terraform**. El nombre de la red, el volumen y el contenedor incorpora el entorno. Una operación de desarrollo no debe alterar PostgreSQL de producción.

| Ambiente | Red Docker | Volumen PostgreSQL | Contenedor PostgreSQL |
|---|---|---|---|
| dev | `gestor-franquicias-dev-red` | `gestor-franquicias-dev-datos` | `gestor-franquicias-dev-postgres` |
| test | `gestor-franquicias-test-red` | `gestor-franquicias-test-datos` | `gestor-franquicias-test-postgres` |
| stage | `gestor-franquicias-stage-red` | `gestor-franquicias-stage-datos` | `gestor-franquicias-stage-postgres` |
| prod | `gestor-franquicias-prod-red` | `gestor-franquicias-prod-datos` | `gestor-franquicias-prod-postgres` |

El workflow de cada ambiente inicializa y valida Terraform, genera un plan e inspecciona que los cambios solo afecten sus recursos. Rechaza operaciones que eliminen el volumen persistente.

El estado Terraform no debe sincronizarse con Git: contiene información sensible y debe respaldarse con permisos restringidos. Nunca ejecutar `terraform destroy` automáticamente.

El despliegue real necesita secretos SSH en los entornos de GitHub. No se ha ejecutado todavía en el VPS.

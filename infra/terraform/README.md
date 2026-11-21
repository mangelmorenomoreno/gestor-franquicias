# Terraform

Terraform configura la infraestructura Docker utilizada por la aplicación en el VPS: una red privada, un volumen y un contenedor PostgreSQL 16.

En el servidor solo se ejecuta **`prod`**. Los otros perfiles cuentan con archivos de configuración, pero no están desplegados.

## Archivos

- `main.tf`: red, volumen y contenedor PostgreSQL.
- `variables.tf`: ambiente, base de datos, usuario y contraseña.
- `versions.tf`: versión de Terraform y del proveedor Docker.
- `terraform.tfvars.example`: valores de referencia para configurar un ambiente.

El estado de producción se administra en `/opt/gestor-franquicias/prod/infra/terraform`.

## Validar la configuración

Con Terraform instalado, desde la raíz del proyecto:

```bash
terraform -chdir=infra/terraform init -backend=false -input=false
terraform -chdir=infra/terraform validate -no-color
```

No guardar contraseñas ni archivos de estado de Terraform en Git. El volumen de PostgreSQL contiene los datos de la aplicación y no debe eliminarse durante una actualización.

Para ejecutar el proyecto **localmente no se necesita Terraform**; utilizar [Docker Compose o Maven](../../README.md#ejecutar-localmente).

El flujo de publicación y la configuración SSH se describen en [Despliegue](../../docs/DESPLIEGUE.md).

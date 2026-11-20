# Infraestructura de PostgreSQL

La base de datos se puede aprovisionar con Terraform en un entorno aislado del VPS. Terraform administra únicamente la red, el volumen y el contenedor de PostgreSQL de Gestor de Franquicias.

## Aplicación en el servidor

1. Instalar Terraform y confirmar que Docker funciona.
2. Ejecutar los siguientes comandos desde esta carpeta:

~~~bash
terraform init
terraform validate
export TF_VAR_clave_base_datos="CLAVE_PRIVADA"
terraform plan
terraform apply
~~~

3. Con el contenedor de PostgreSQL activo, configurar el archivo .env de la API y levantar el archivo docker-compose.prod.yml.

**Importante:** La contraseña también puede quedar registrada en el estado de Terraform, aunque la variable esté marcada como sensible. El archivo de estado debe quedar privado, fuera de Git y con permisos restringidos. No se debe hacer terraform destroy hasta que termine la evaluación y tengamos respaldo de los datos si queremos conservarlos.

El despliegue todavía no se ha ejecutado sobre el VPS.

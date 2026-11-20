# Despliegue en el VPS

El entorno de producción de esta API tiene su propio directorio, red Docker, volumen PostgreSQL y contenedor. No requiere dominio y se puede consultar desde la IP pública y un puerto dedicado.

## Preparar el VPS

1. Confirmar que Docker, Docker Compose y Terraform están disponibles.
2. Crear el directorio `/opt/gestor-franquicias` y colocar allí `docker-compose.prod.yml` y el archivo `.env` con permisos restringidos.
3. Configurar la contraseña de PostgreSQL y las variables del archivo de entorno.
4. Aprovisionar la base de datos desde `infra/terraform` ejecutando `terraform init`, `terraform plan` y `terraform apply` en el VPS.
5. Verificar que existe la red `gestor-franquicias-red` y que PostgreSQL está disponible solo dentro de esa red Docker.
6. Comprobar que la imagen publicada en GHCR es accesible desde el servidor.
7. Validar `docker compose --env-file .env -f docker-compose.prod.yml config`.

Configuración del archivo `.env` (ejemplo, no utilizar esta contraseña):

~~~dotenv
IMAGEN_BACKEND=ghcr.io/mangelmorenomoreno/gestor-franquicias:latest
PUERTO_API=8088
IP_ESCUCHA_API=127.0.0.1
DB_USERNAME=gestor_franquicias
DB_PASSWORD=CAMBIAR_POR_UNA_CLAVE_PRIVADA
~~~

En el VPS se debe comprobar que el puerto elegido no esté ocupado y que el firewall permita únicamente los accesos necesarios. PostgreSQL no tiene puertos publicados hacia Internet.

## Publicar la imagen

Después de que un commit llegue a `main` y supere la validación, el workflow `Publicar imagen Docker` compila y publica la imagen en GHCR con la etiqueta del commit y la etiqueta `latest`. La publicación debe verificarse antes de intentar desplegar.

Si la imagen queda privada, se requiere iniciar sesión en GHCR en el VPS o configurar el paquete como público.

## Configurar GitHub Actions para el despliegue

Crear el entorno `produccion` en GitHub y configurar estos secretos para ese entorno:

- `VPS_HOST`: dirección del servidor.
- `VPS_USER`: usuario SSH con los permisos indispensables.
- `VPS_SSH_KEY`: clave privada SSH del usuario de despliegue.
- `VPS_KNOWN_HOSTS`: entrada de llave pública del host SSH verificada previamente por un canal confiable.

Las credenciales reales nunca deben escribirse en el repositorio ni compartirse en un mensaje de chat. La clave de host debe verificarse antes de guardarla; no deshabilitar `StrictHostKeyChecking`.

Una vez configurados, ejecutar manualmente el workflow `Desplegar API en el VPS` desde la rama `main`. Esta tarea actualiza solo el servicio `api` dentro del proyecto de Compose que se encuentra en `/opt/gestor-franquicias`.

La opción `IP_ESCUCHA_API=127.0.0.1` mantiene la API accesible solo desde el servidor por defecto. Recomiendo identificar la imagen por la etiqueta inmutable del commit en lugar de `latest` para mantener la trazabilidad de la entrega.

El despliegue está pensado como operación independiente del proceso de publicación. No se ejecutará automáticamente en el VPS hasta que esté preparada y validada su infraestructura.

## Verificar el resultado

~~~bash
docker compose --env-file .env -f docker-compose.prod.yml ps
docker compose --env-file .env -f docker-compose.prod.yml logs --tail=100 api
curl -fsS http://127.0.0.1:8088/actuator/health
~~~

Desde otro equipo, si el puerto está habilitado, la documentación se encuentra en `http://IP_PUBLICA:8088/swagger-ui.html`. El acceso por IP y HTTP no está cifrado. Como esta prueba no exige autenticación, los endpoints de escritura deben quedar restringidos mediante firewall a direcciones autorizadas. Para publicar por IP, usar `IP_ESCUCHA_API=0.0.0.0` solamente después de configurar esa restricción; para un acceso más prolongado es preferible HTTPS.

## Retirar la prueba

Después de finalizar la evaluación, comprobar que no se requiere conservar la información y preparar una copia de seguridad cuando aplique. Detener únicamente los contenedores del proyecto y, si corresponde, ejecutar `terraform destroy` desde su carpeta de infraestructura.

No se debe usar `docker system prune --volumes` ni eliminar redes o volúmenes de otras aplicaciones.

**Estado actual:** el despliegue real y la configuración de secretos están pendientes de comprobarse.

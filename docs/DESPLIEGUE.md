# Despliegue

La aplicación utiliza GitHub Actions, GHCR, Docker y Terraform. El despliegue automático se realiza **únicamente desde `main` hacia producción**.

## Ambientes

Los puertos de desarrollo, pruebas y preproducción están configurados para posibles instalaciones separadas. En este VPS **solo se ejecuta `prod`**.

| Perfil | Rama | Puerto configurado | VPS |
| --- | --- | --- | --- |
| `dev` | `dev` | 8086 | No desplegado |
| `test` | `test` | 8087 | No desplegado |
| `stage` | `stage` | 8089 | No desplegado |
| `prod` | `main` | 8088 | Desplegado |

La API de producción está disponible en http://2.25.123.13:8088/swagger-ui/index.html. PostgreSQL permanece en una red Docker privada.

## Flujo de publicación

1. Los cambios en `main` activan la compilación y las pruebas de GitHub Actions.
2. Al finalizar correctamente la validación, se publica en GHCR una imagen etiquetada con la revisión del código.
3. El workflow de producción transfiere la configuración al VPS e inicia el despliegue.
4. Terraform administra la red, el volumen y PostgreSQL. Docker Compose levanta la API.
5. Se comprueba la salud de la aplicación y la disponibilidad de las rutas HTTP de lectura y Swagger.

El código de la aplicación se empaqueta mediante `Dockerfile.runtime`. Para desarrollo local se utiliza el `Dockerfile` multietapa que compila el proyecto desde sus fuentes.

Los archivos `docker-compose.dev.yml`, `docker-compose.test.yml` y `docker-compose.stage.yml` permiten configurar cada perfil, pero su existencia no implica que esos ambientes estén desplegados.

## Configuración en GitHub

En `Settings → Environments`, el ambiente `produccion` debe permitir despliegues desde `main` y contener los siguientes secretos:

| Secreto | Uso |
| --- | --- |
| `VPS_HOST` | Dirección del servidor |
| `VPS_USER` | Usuario SSH |
| `VPS_SSH_KEY` | Clave privada SSH |
| `VPS_KNOWN_HOSTS` | Clave pública de host verificada |
| `DB_PASSWORD` | Contraseña de PostgreSQL |

Si GHCR requiere autenticación privada, se configuran también `GHCR_USUARIO` y `GHCR_LEER_PAQUETE`. No guardar credenciales en el repositorio.

La variable opcional `VPS_PUBLIC_HOST` permite definir la dirección que utiliza el workflow para probar los endpoints públicos.

## Producción en el VPS

Ruta de instalación: `/opt/gestor-franquicias/prod`.

Verificar en el servidor:

```bash
docker ps --filter name=gestor-franquicias-prod
curl -fsS http://127.0.0.1:8088/actuator/health
```

Swagger y OpenAPI:

```text
http://2.25.123.13:8088/swagger-ui/index.html
http://2.25.123.13:8088/v3/api-docs
```

La API escucha en `0.0.0.0:8088` y el puerto está permitido en UFW cuando se encuentra activo. Si el proveedor administra un firewall adicional, también debe permitir TCP 8088.

Terraform conserva su estado por ambiente y el despliegue no ejecuta `terraform destroy`. El volumen PostgreSQL no debe eliminarse al actualizar la aplicación.

## Ejecución local

No se necesita acceso al VPS ni credenciales de GitHub para trabajar en el equipo de desarrollo. El procedimiento con **Docker Compose** y con **Maven más PostgreSQL en Docker** está descrito en el [README](../README.md#ejecutar-localmente).

**Seguridad:** el acceso público actual utiliza HTTP y no exige autenticación para modificar información. Usar solo datos de demostración. Para datos reales se requiere HTTPS y control de acceso.

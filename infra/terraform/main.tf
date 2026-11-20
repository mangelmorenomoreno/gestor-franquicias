resource "docker_network" "franquicias" {
  name = "gestor-franquicias-red"
}

resource "docker_volume" "datos_franquicias" {
  name = "gestor-franquicias-datos"
}

resource "docker_image" "postgres" {
  name         = "postgres:16-alpine"
  keep_locally = true
}

resource "docker_container" "postgres" {
  name    = "gestor-franquicias-postgres"
  image   = docker_image.postgres.image_id
  restart = "unless-stopped"

  env = [
    "POSTGRES_DB=${var.nombre_base_datos}",
    "POSTGRES_USER=${var.usuario_base_datos}",
    "POSTGRES_PASSWORD=${var.clave_base_datos}"
  ]

  networks_advanced {
    name    = docker_network.franquicias.name
    aliases = ["base-datos"]
  }

  volumes {
    volume_name    = docker_volume.datos_franquicias.name
    container_path = "/var/lib/postgresql/data"
  }

  healthcheck {
    test     = ["CMD-SHELL", "pg_isready -U ${var.usuario_base_datos} -d ${var.nombre_base_datos}"]
    interval = "10s"
    timeout  = "5s"
    retries  = 5
  }
}

variable "ambiente" {
  description = "Entorno aislado de infraestructura: dev, test, stage o prod."
  type        = string
  validation {
    condition     = contains(["dev", "test", "stage", "prod"], var.ambiente)
    error_message = "El ambiente debe ser dev, test, stage o prod."
  }
}

variable "nombre_base_datos" {
  description = "Nombre de PostgreSQL dentro de cada contenedor independiente."
  type        = string
  default     = "gestor_franquicias"
}

variable "usuario_base_datos" {
  description = "Usuario exclusivo de la base PostgreSQL del entorno."
  type        = string
  default     = "gestor_franquicias"
}

variable "clave_base_datos" {
  description = "Contraseña privada del entorno. Nunca colocar en Git."
  type        = string
  sensitive   = true
}

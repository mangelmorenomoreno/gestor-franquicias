variable "nombre_base_datos" {
  description = "Nombre de la base de datos independiente para la prueba."
  type        = string
  default     = "gestor_franquicias"
}

variable "usuario_base_datos" {
  description = "Usuario que utilizará únicamente la API de franquicias."
  type        = string
  default     = "gestor_franquicias"
}

variable "clave_base_datos" {
  description = "Contraseña de PostgreSQL. No se debe registrar en el repositorio."
  type        = string
  sensitive   = true
}

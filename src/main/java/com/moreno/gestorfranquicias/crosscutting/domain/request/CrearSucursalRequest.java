package com.moreno.gestorfranquicias.crosscutting.domain.request;
import jakarta.validation.constraints.*;
public record CrearSucursalRequest(@NotBlank @Size(max = 120) String nombre) {}

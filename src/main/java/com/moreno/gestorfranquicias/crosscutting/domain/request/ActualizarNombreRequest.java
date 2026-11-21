package com.moreno.gestorfranquicias.crosscutting.domain.request;

import jakarta.validation.constraints.*;

public record ActualizarNombreRequest(@NotBlank @Size(max = 120) String nombre) {}

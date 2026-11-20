package com.moreno.gestorfranquicias.crosscutting.domain.request;
import jakarta.validation.constraints.*;
public record CrearFranquiciaRequest(@NotBlank @Size(max = 120) String nombre) {}

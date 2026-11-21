package com.moreno.gestorfranquicias.crosscutting.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearSucursalRequest(@NotBlank @Size(max = 120) String nombre) {

}

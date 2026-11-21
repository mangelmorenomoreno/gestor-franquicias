package com.moreno.gestorfranquicias.crosscutting.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CrearProductoRequest(
        @NotBlank @Size(max = 120) String nombre, @NotNull @PositiveOrZero Integer stock) {

}

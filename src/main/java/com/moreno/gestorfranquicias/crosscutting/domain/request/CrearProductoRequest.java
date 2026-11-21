package com.moreno.gestorfranquicias.crosscutting.domain.request;

import jakarta.validation.constraints.*;

public record CrearProductoRequest(
        @NotBlank @Size(max = 120) String nombre, @NotNull @PositiveOrZero Integer stock) {}

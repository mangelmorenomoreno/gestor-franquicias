package com.moreno.gestorfranquicias.crosscutting.domain.request;

import jakarta.validation.constraints.*;

public record ActualizarStockRequest(@NotNull @PositiveOrZero Integer stock) {}

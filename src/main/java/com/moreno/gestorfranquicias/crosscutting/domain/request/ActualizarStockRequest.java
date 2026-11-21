package com.moreno.gestorfranquicias.crosscutting.domain.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ActualizarStockRequest(@NotNull @PositiveOrZero Integer stock) {

}

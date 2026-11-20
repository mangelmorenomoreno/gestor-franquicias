package com.moreno.gestorfranquicias.crosscutting.domain.common;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
public record ApiResponse<T>(String code, String message, boolean success,
        T data, List<DetalleError> errors, OffsetDateTime timestamp) {
    public static <T> ApiResponse<T> correcto(String mensaje, T datos) {
        return new ApiResponse<>("200", mensaje, true, datos, List.of(), OffsetDateTime.now(ZoneOffset.UTC));
    }
    public static <T> ApiResponse<T> creado(String mensaje, T datos) {
        return new ApiResponse<>("201", mensaje, true, datos, List.of(), OffsetDateTime.now(ZoneOffset.UTC));
    }
    public static <T> ApiResponse<T> error(String codigo, String mensaje, List<DetalleError> detalles) {
        return new ApiResponse<>(codigo, mensaje, false, null, detalles, OffsetDateTime.now(ZoneOffset.UTC));
    }
}

package com.moreno.gestorfranquicias.crosscutting.domain.response;

public record SucursalDetalleResponse(
        Long idSucursal,
        Long idFranquicia,
        String nombre,
        java.util.List<ProductoResponse> productos) {

}

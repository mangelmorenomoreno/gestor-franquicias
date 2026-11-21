package com.moreno.gestorfranquicias.crosscutting.domain.response;

public record ProductoResponse(Long idProducto, Long idSucursal, String nombre, Integer stock) {

}

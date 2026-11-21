package com.moreno.gestorfranquicias.crosscutting.domain.response;

public record ProductoMayorStockResponse(
    Long idSucursal,
    String nombreSucursal,
    Long idProducto,
    String nombreProducto,
    Integer stock) {}

package com.moreno.gestorfranquicias.crosscutting.domain.response;

public record FranquiciaDetalleResponse(
        Long idFranquicia, String nombre, java.util.List<SucursalResumenResponse> sucursales) {}

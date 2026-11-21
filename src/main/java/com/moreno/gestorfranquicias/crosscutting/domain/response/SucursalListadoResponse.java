package com.moreno.gestorfranquicias.crosscutting.domain.response;

/** Identifica la franquicia propietaria de una sucursal en consultas globales. */
public record SucursalListadoResponse(Long idSucursal, Long idFranquicia, String nombre) {}

package com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.mapper;

import com.moreno.gestorfranquicias.crosscutting.domain.response.SucursalListadoResponse;
import com.moreno.gestorfranquicias.crosscutting.domain.response.SucursalResumenResponse;
import com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.entity.SucursalEntity;
import org.springframework.stereotype.Component;

@Component
public class SucursalMapper {
  public SucursalResumenResponse convertir(SucursalEntity entidad) {
    return new SucursalResumenResponse(entidad.idSucursal(), entidad.nombre());
  }

  public SucursalListadoResponse convertirListado(SucursalEntity entidad) {
    return new SucursalListadoResponse(
        entidad.idSucursal(), entidad.idFranquicia(), entidad.nombre());
  }
}

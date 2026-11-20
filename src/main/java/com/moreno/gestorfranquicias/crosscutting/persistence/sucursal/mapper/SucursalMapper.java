package com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.mapper;
import com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.entity.SucursalEntity;
import com.moreno.gestorfranquicias.crosscutting.domain.response.SucursalResumenResponse;
import org.springframework.stereotype.Component;
@Component
public class SucursalMapper { public SucursalResumenResponse convertir(SucursalEntity entidad) { return new SucursalResumenResponse(entidad.idSucursal(), entidad.nombre()); } }

package com.moreno.gestorfranquicias.crosscutting.persistence.producto.mapper;

import com.moreno.gestorfranquicias.crosscutting.domain.response.ProductoResponse;
import com.moreno.gestorfranquicias.crosscutting.persistence.producto.entity.ProductoEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {
  public ProductoResponse convertir(ProductoEntity entidad) {
    return new ProductoResponse(
        entidad.idProducto(), entidad.idSucursal(), entidad.nombre(), entidad.stock());
  }
}

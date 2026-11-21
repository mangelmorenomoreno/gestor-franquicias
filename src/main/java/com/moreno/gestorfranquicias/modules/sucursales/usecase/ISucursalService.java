package com.moreno.gestorfranquicias.modules.sucursales.usecase;

import com.moreno.gestorfranquicias.crosscutting.domain.request.*;
import com.moreno.gestorfranquicias.crosscutting.domain.response.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ISucursalService {

  Mono<SucursalResumenResponse> crear(Long idFranquicia, CrearSucursalRequest datos);

  Mono<SucursalDetalleResponse> consultar(Long idSucursal);

  Flux<SucursalListadoResponse> listar();

  Flux<ProductoResponse> listarProductos(Long idSucursal);

  Mono<SucursalResumenResponse> cambiarNombre(Long idSucursal, ActualizarNombreRequest datos);
}

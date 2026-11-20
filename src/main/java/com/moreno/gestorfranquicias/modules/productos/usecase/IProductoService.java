package com.moreno.gestorfranquicias.modules.productos.usecase;
import com.moreno.gestorfranquicias.crosscutting.domain.request.*;
import com.moreno.gestorfranquicias.crosscutting.domain.response.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
public interface IProductoService {

Mono<ProductoResponse> crear(Long idSucursal, CrearProductoRequest datos);
Mono<ProductoResponse> consultar(Long idSucursal, Long idProducto);
Mono<ProductoResponse> cambiarStock(Long idSucursal, Long idProducto, ActualizarStockRequest datos);
Mono<ProductoResponse> cambiarNombre(Long idSucursal, Long idProducto, ActualizarNombreRequest datos);
Mono<Void> eliminar(Long idSucursal, Long idProducto);
}

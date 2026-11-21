package com.moreno.gestorfranquicias.modules.productos.dataproviders;

import com.moreno.gestorfranquicias.crosscutting.domain.response.ProductoMayorStockResponse;
import com.moreno.gestorfranquicias.crosscutting.persistence.producto.entity.ProductoEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IProductoDataProvider {
  Mono<ProductoEntity> buscar(Long idProducto);

  Flux<ProductoEntity> listarPorSucursal(Long idSucursal);

  Flux<ProductoEntity> listar();

  Mono<ProductoEntity> guardar(ProductoEntity producto);

  Mono<Void> eliminar(ProductoEntity producto);

  Flux<ProductoMayorStockResponse> obtenerMayorStockPorSucursal(Long idFranquicia);
}

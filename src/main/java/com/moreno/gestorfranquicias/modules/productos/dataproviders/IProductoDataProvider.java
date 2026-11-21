package com.moreno.gestorfranquicias.modules.productos.dataproviders;
import com.moreno.gestorfranquicias.crosscutting.persistence.producto.entity.ProductoEntity;
import com.moreno.gestorfranquicias.crosscutting.domain.response.ProductoMayorStockResponse;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
public interface IProductoDataProvider { Mono<ProductoEntity> buscar(Long idProducto); Flux<ProductoEntity> listarPorSucursal(Long idSucursal); Flux<ProductoEntity> listar(); Mono<ProductoEntity> guardar(ProductoEntity producto); Mono<Void> eliminar(ProductoEntity producto); Flux<ProductoMayorStockResponse> obtenerMayorStockPorSucursal(Long idFranquicia); }

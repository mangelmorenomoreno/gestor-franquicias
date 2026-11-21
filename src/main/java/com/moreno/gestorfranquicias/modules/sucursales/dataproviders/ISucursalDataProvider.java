package com.moreno.gestorfranquicias.modules.sucursales.dataproviders;
import com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.entity.SucursalEntity;
import com.moreno.gestorfranquicias.crosscutting.domain.response.ProductoMayorStockResponse;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
public interface ISucursalDataProvider { Mono<SucursalEntity> buscar(Long idSucursal); Flux<SucursalEntity> listarPorFranquicia(Long idFranquicia); Flux<SucursalEntity> listar(); Mono<SucursalEntity> guardar(SucursalEntity sucursal); }

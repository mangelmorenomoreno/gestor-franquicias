package com.moreno.gestorfranquicias.modules.franquicias.dataproviders;
import com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.entity.FranquiciaEntity;
import com.moreno.gestorfranquicias.crosscutting.domain.response.ProductoMayorStockResponse;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
public interface IFranquiciaDataProvider { Mono<FranquiciaEntity> buscar(Long idFranquicia); Flux<FranquiciaEntity> listar(); Mono<FranquiciaEntity> guardar(FranquiciaEntity franquicia); }

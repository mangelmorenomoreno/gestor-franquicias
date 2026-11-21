package com.moreno.gestorfranquicias.modules.franquicias.dataproviders;

import com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.entity.FranquiciaEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IFranquiciaDataProvider {
  Mono<FranquiciaEntity> buscar(Long idFranquicia);

  Flux<FranquiciaEntity> listar();

  Mono<FranquiciaEntity> guardar(FranquiciaEntity franquicia);
}

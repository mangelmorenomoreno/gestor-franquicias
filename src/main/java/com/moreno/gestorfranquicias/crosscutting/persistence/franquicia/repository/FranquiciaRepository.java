package com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.repository;

import com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.entity.FranquiciaEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface FranquiciaRepository extends ReactiveCrudRepository<FranquiciaEntity, Long> {
  Flux<FranquiciaEntity> findAllByOrderByIdFranquiciaAsc();
}

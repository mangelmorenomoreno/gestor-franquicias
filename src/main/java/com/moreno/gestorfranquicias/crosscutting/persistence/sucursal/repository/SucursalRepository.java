package com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.repository;
import com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.entity.SucursalEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
public interface SucursalRepository extends ReactiveCrudRepository<SucursalEntity, Long> {
    Flux<SucursalEntity> findByIdFranquiciaOrderByIdSucursalAsc(Long idFranquicia);
    Flux<SucursalEntity> findAllByOrderByIdSucursalAsc();
}

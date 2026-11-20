package com.moreno.gestorfranquicias.crosscutting.persistence.producto.repository;
import com.moreno.gestorfranquicias.crosscutting.persistence.producto.entity.ProductoEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
public interface ProductoRepository extends ReactiveCrudRepository<ProductoEntity, Long> { Flux<ProductoEntity> findByIdSucursalOrderByIdProductoAsc(Long idSucursal); }

package com.moreno.gestorfranquicias.modules.sucursales.dataproviders.r2dbc;
import com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.entity.SucursalEntity;
import com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.repository.SucursalRepository;
import com.moreno.gestorfranquicias.modules.sucursales.dataproviders.ISucursalDataProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
@Component
public class SucursalDataProvider implements ISucursalDataProvider {
    private final SucursalRepository repositorio;
    public SucursalDataProvider(SucursalRepository repositorio) { this.repositorio = repositorio; }
    public Mono<SucursalEntity> buscar(Long idSucursal) { return repositorio.findById(idSucursal); }
    public Flux<SucursalEntity> listar() { return repositorio.findAllByOrderByIdSucursalAsc(); }
    public Flux<SucursalEntity> listarPorFranquicia(Long idFranquicia) {
        return repositorio.findByIdFranquiciaOrderByIdSucursalAsc(idFranquicia);
    }
    public Mono<SucursalEntity> guardar(SucursalEntity sucursal) { return repositorio.save(sucursal); }
}

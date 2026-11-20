package com.moreno.gestorfranquicias.modules.franquicias.dataproviders.r2dbc;
import com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.entity.FranquiciaEntity;
import com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.repository.FranquiciaRepository;
import com.moreno.gestorfranquicias.modules.franquicias.dataproviders.IFranquiciaDataProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
@Component
public class FranquiciaDataProvider implements IFranquiciaDataProvider {
    private final FranquiciaRepository repositorio;
    public FranquiciaDataProvider(FranquiciaRepository repositorio) { this.repositorio = repositorio; }
    public Mono<FranquiciaEntity> buscar(Long idFranquicia) { return repositorio.findById(idFranquicia); }
    public Flux<FranquiciaEntity> listar() { return repositorio.findAllByOrderByIdFranquiciaAsc(); }
    public Mono<FranquiciaEntity> guardar(FranquiciaEntity franquicia) { return repositorio.save(franquicia); }
}

package com.moreno.gestorfranquicias.modules.franquicias.usecase;

import com.moreno.gestorfranquicias.crosscutting.domain.request.*;
import com.moreno.gestorfranquicias.crosscutting.domain.response.*;
import com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.entity.FranquiciaEntity;
import com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.mapper.FranquiciaMapper;
import com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.mapper.SucursalMapper;
import com.moreno.gestorfranquicias.infrastructure.exception.ExcepcionNegocio;
import com.moreno.gestorfranquicias.modules.franquicias.dataproviders.IFranquiciaDataProvider;
import com.moreno.gestorfranquicias.modules.sucursales.dataproviders.ISucursalDataProvider;
import com.moreno.gestorfranquicias.modules.productos.dataproviders.IProductoDataProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class FranquiciaService implements IFranquiciaService {
    private final IFranquiciaDataProvider franquicias;
    private final ISucursalDataProvider sucursales;
    private final IProductoDataProvider productos;
    private final FranquiciaMapper franquiciaMapper;
    private final SucursalMapper sucursalMapper;

    public FranquiciaService(IFranquiciaDataProvider franquicias, ISucursalDataProvider sucursales,
            IProductoDataProvider productos, FranquiciaMapper franquiciaMapper, SucursalMapper sucursalMapper) {
        this.franquicias = franquicias;
        this.sucursales = sucursales;
        this.productos = productos;
        this.franquiciaMapper = franquiciaMapper;
        this.sucursalMapper = sucursalMapper;
    }

    @Transactional
    public Mono<FranquiciaResumenResponse> crear(CrearFranquiciaRequest datos) {
        return franquicias.guardar(new FranquiciaEntity(null, datos.nombre().trim()))
                .map(franquiciaMapper::convertir);
    }

    public Flux<FranquiciaResumenResponse> listar() {
        return franquicias.listar().map(franquiciaMapper::convertir);
    }

    public Mono<FranquiciaDetalleResponse> consultar(Long idFranquicia) {
        return buscarFranquicia(idFranquicia)
                .flatMap(franquicia -> sucursales.listarPorFranquicia(idFranquicia)
                        .map(sucursalMapper::convertir)
                        .collectList()
                        .map(lista -> new FranquiciaDetalleResponse(
                                franquicia.idFranquicia(), franquicia.nombre(), lista)));
    }

    public Flux<SucursalResumenResponse> listarSucursales(Long idFranquicia) {
        return buscarFranquicia(idFranquicia)
                .thenMany(sucursales.listarPorFranquicia(idFranquicia).map(sucursalMapper::convertir));
    }

    public Flux<ProductoMayorStockResponse> obtenerMayorStock(Long idFranquicia) {
        return buscarFranquicia(idFranquicia)
                .thenMany(productos.obtenerMayorStockPorSucursal(idFranquicia));
    }

    @Transactional
    public Mono<FranquiciaResumenResponse> cambiarNombre(Long idFranquicia, ActualizarNombreRequest datos) {
        return buscarFranquicia(idFranquicia)
                .flatMap(actual -> franquicias.guardar(new FranquiciaEntity(actual.idFranquicia(), datos.nombre().trim())))
                .map(franquiciaMapper::convertir);
    }

    private Mono<FranquiciaEntity> buscarFranquicia(Long idFranquicia) {
        return franquicias.buscar(idFranquicia)
                .switchIfEmpty(Mono.error(ExcepcionNegocio.noEncontrado("La franquicia")));
    }
}

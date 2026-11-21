package com.moreno.gestorfranquicias.modules.franquicias.usecase;

import com.moreno.gestorfranquicias.crosscutting.domain.request.ActualizarNombreRequest;
import com.moreno.gestorfranquicias.crosscutting.domain.request.CrearFranquiciaRequest;
import com.moreno.gestorfranquicias.crosscutting.domain.response.FranquiciaDetalleResponse;
import com.moreno.gestorfranquicias.crosscutting.domain.response.FranquiciaResumenResponse;
import com.moreno.gestorfranquicias.crosscutting.domain.response.ProductoMayorStockResponse;
import com.moreno.gestorfranquicias.crosscutting.domain.response.SucursalResumenResponse;
import com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.entity.FranquiciaEntity;
import com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.mapper.FranquiciaMapper;
import com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.mapper.SucursalMapper;
import com.moreno.gestorfranquicias.infrastructure.exception.ExcepcionNegocio;
import com.moreno.gestorfranquicias.modules.franquicias.dataproviders.IFranquiciaDataProvider;
import com.moreno.gestorfranquicias.modules.productos.dataproviders.IProductoDataProvider;
import com.moreno.gestorfranquicias.modules.sucursales.dataproviders.ISucursalDataProvider;

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

    public FranquiciaService(
            IFranquiciaDataProvider franquicias,
            ISucursalDataProvider sucursales,
            IProductoDataProvider productos,
            FranquiciaMapper franquiciaMapper,
            SucursalMapper sucursalMapper) {
        this.franquicias = franquicias;
        this.sucursales = sucursales;
        this.productos = productos;
        this.franquiciaMapper = franquiciaMapper;
        this.sucursalMapper = sucursalMapper;
    }

    /** Mono: el guardado devuelve una sola franquicia. */
    @Override
    @Transactional
    public Mono<FranquiciaResumenResponse> crear(CrearFranquiciaRequest datos) {
        return franquicias
                .guardar(new FranquiciaEntity(null, datos.nombre().trim()))
                .map(franquiciaMapper::convertir);
    }

    /** Flux: se consultan varias franquicias sin bloquear el hilo. */
    @Override
    public Flux<FranquiciaResumenResponse> listar() {
        return franquicias.listar().map(franquiciaMapper::convertir);
    }

    /** Mono: reúne una franquicia y la lista de sus sucursales. */
    @Override
    public Mono<FranquiciaDetalleResponse> consultar(Long idFranquicia) {
        return buscarFranquicia(idFranquicia)
                .flatMap(
                        franquicia ->
                                sucursales
                                        .listarPorFranquicia(idFranquicia)
                                        .map(sucursalMapper::convertir)
                                        .collectList()
                                        .map(
                                                lista ->
                                                        new FranquiciaDetalleResponse(
                                                                franquicia.idFranquicia(),
                                                                franquicia.nombre(),
                                                                lista)));
    }

    /** Flux: una franquicia puede tener varias sucursales. */
    @Override
    public Flux<SucursalResumenResponse> listarSucursales(Long idFranquicia) {
        return buscarFranquicia(idFranquicia)
                .thenMany(
                        sucursales
                                .listarPorFranquicia(idFranquicia)
                                .map(sucursalMapper::convertir));
    }

    /** Flux: devuelve el producto principal de cada sucursal. */
    @Override
    public Flux<ProductoMayorStockResponse> obtenerMayorStock(Long idFranquicia) {
        return buscarFranquicia(idFranquicia)
                .thenMany(productos.obtenerMayorStockPorSucursal(idFranquicia));
    }

    /** Mono: actualiza y devuelve una sola franquicia. */
    @Override
    @Transactional
    public Mono<FranquiciaResumenResponse> cambiarNombre(
            Long idFranquicia, ActualizarNombreRequest datos) {
        return buscarFranquicia(idFranquicia)
                .flatMap(
                        actual ->
                                franquicias.guardar(
                                        new FranquiciaEntity(
                                                actual.idFranquicia(), datos.nombre().trim())))
                .map(franquiciaMapper::convertir);
    }

    /** Mono: devuelve la franquicia o un error cuando no existe. */
    private Mono<FranquiciaEntity> buscarFranquicia(Long idFranquicia) {
        return franquicias
                .buscar(idFranquicia)
                .switchIfEmpty(Mono.error(ExcepcionNegocio.noEncontrado("La franquicia")));
    }
}

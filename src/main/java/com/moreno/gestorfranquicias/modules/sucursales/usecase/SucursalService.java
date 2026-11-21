package com.moreno.gestorfranquicias.modules.sucursales.usecase;

import com.moreno.gestorfranquicias.crosscutting.domain.request.ActualizarNombreRequest;
import com.moreno.gestorfranquicias.crosscutting.domain.request.CrearSucursalRequest;
import com.moreno.gestorfranquicias.crosscutting.domain.response.ProductoResponse;
import com.moreno.gestorfranquicias.crosscutting.domain.response.SucursalDetalleResponse;
import com.moreno.gestorfranquicias.crosscutting.domain.response.SucursalListadoResponse;
import com.moreno.gestorfranquicias.crosscutting.domain.response.SucursalResumenResponse;
import com.moreno.gestorfranquicias.crosscutting.persistence.producto.mapper.ProductoMapper;
import com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.entity.SucursalEntity;
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
public class SucursalService implements ISucursalService {
    private final IFranquiciaDataProvider franquicias;
    private final ISucursalDataProvider sucursales;
    private final IProductoDataProvider productos;
    private final SucursalMapper sucursalMapper;
    private final ProductoMapper productoMapper;

    public SucursalService(
            IFranquiciaDataProvider franquicias,
            ISucursalDataProvider sucursales,
            IProductoDataProvider productos,
            SucursalMapper sucursalMapper,
            ProductoMapper productoMapper) {
        this.franquicias = franquicias;
        this.sucursales = sucursales;
        this.productos = productos;
        this.sucursalMapper = sucursalMapper;
        this.productoMapper = productoMapper;
    }

    /** Mono: el alta corresponde a una sola sucursal. */
    @Override
    @Transactional
    public Mono<SucursalResumenResponse> crear(Long idFranquicia, CrearSucursalRequest datos) {
        return franquicias
                .buscar(idFranquicia)
                .switchIfEmpty(Mono.error(ExcepcionNegocio.noEncontrado("La franquicia")))
                .then(
                        sucursales.guardar(
                                new SucursalEntity(null, idFranquicia, datos.nombre().trim())))
                .map(sucursalMapper::convertir);
    }

    /** Flux: devuelve las sucursales disponibles. */
    @Override
    public Flux<SucursalListadoResponse> listar() {
        return sucursales.listar().map(sucursalMapper::convertirListado);
    }

    /** Mono: agrupa una sucursal y sus productos. */
    @Override
    public Mono<SucursalDetalleResponse> consultar(Long idSucursal) {
        return buscarSucursal(idSucursal)
                .flatMap(
                        sucursal ->
                                productos
                                        .listarPorSucursal(idSucursal)
                                        .map(productoMapper::convertir)
                                        .collectList()
                                        .map(
                                                lista ->
                                                        new SucursalDetalleResponse(
                                                                sucursal.idSucursal(),
                                                                sucursal.idFranquicia(),
                                                                sucursal.nombre(),
                                                                lista)));
    }

    /** Flux: una sucursal puede registrar varios productos. */
    @Override
    public Flux<ProductoResponse> listarProductos(Long idSucursal) {
        return buscarSucursal(idSucursal)
                .thenMany(productos.listarPorSucursal(idSucursal).map(productoMapper::convertir));
    }

    /** Mono: cambia el nombre de una sucursal. */
    @Override
    @Transactional
    public Mono<SucursalResumenResponse> cambiarNombre(
            Long idSucursal, ActualizarNombreRequest datos) {
        return buscarSucursal(idSucursal)
                .flatMap(
                        actual ->
                                sucursales.guardar(
                                        new SucursalEntity(
                                                actual.idSucursal(),
                                                actual.idFranquicia(),
                                                datos.nombre().trim())))
                .map(sucursalMapper::convertir);
    }

    /** Mono: busca la sucursal y valida que exista. */
    private Mono<SucursalEntity> buscarSucursal(Long idSucursal) {
        return sucursales
                .buscar(idSucursal)
                .switchIfEmpty(Mono.error(ExcepcionNegocio.noEncontrado("La sucursal")));
    }
}

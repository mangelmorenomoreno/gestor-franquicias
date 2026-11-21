package com.moreno.gestorfranquicias.modules.productos.usecase;

import com.moreno.gestorfranquicias.crosscutting.domain.request.*;
import com.moreno.gestorfranquicias.crosscutting.domain.response.ProductoResponse;
import com.moreno.gestorfranquicias.crosscutting.persistence.producto.entity.ProductoEntity;
import com.moreno.gestorfranquicias.crosscutting.persistence.producto.mapper.ProductoMapper;
import com.moreno.gestorfranquicias.infrastructure.exception.ExcepcionNegocio;
import com.moreno.gestorfranquicias.modules.productos.dataproviders.IProductoDataProvider;
import com.moreno.gestorfranquicias.modules.sucursales.dataproviders.ISucursalDataProvider;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductoService implements IProductoService {
    private final IProductoDataProvider productos;
    private final ISucursalDataProvider sucursales;
    private final ProductoMapper mapper;

    public ProductoService(
            IProductoDataProvider productos,
            ISucursalDataProvider sucursales,
            ProductoMapper mapper) {
        this.productos = productos;
        this.sucursales = sucursales;
        this.mapper = mapper;
    }

    @Transactional
    public Mono<ProductoResponse> crear(Long idSucursal, CrearProductoRequest datos) {
        return sucursales
                .buscar(idSucursal)
                .switchIfEmpty(Mono.error(ExcepcionNegocio.noEncontrado("La sucursal")))
                .then(
                        productos.guardar(
                                new ProductoEntity(
                                        null,
                                        idSucursal,
                                        datos.nombre().trim(),
                                        datos.stock(),
                                        null)))
                .map(mapper::convertir);
    }

    public Flux<ProductoResponse> listar() {
        return productos.listar().map(mapper::convertir);
    }

    public Mono<ProductoResponse> consultar(Long idSucursal, Long idProducto) {
        return buscarProducto(idSucursal, idProducto).map(mapper::convertir);
    }

    @Transactional
    public Mono<ProductoResponse> cambiarStock(
            Long idSucursal, Long idProducto, ActualizarStockRequest datos) {
        return buscarProducto(idSucursal, idProducto)
                .flatMap(
                        actual ->
                                productos.guardar(
                                        new ProductoEntity(
                                                actual.idProducto(),
                                                actual.idSucursal(),
                                                actual.nombre(),
                                                datos.stock(),
                                                actual.version())))
                .map(mapper::convertir);
    }

    @Transactional
    public Mono<ProductoResponse> cambiarNombre(
            Long idSucursal, Long idProducto, ActualizarNombreRequest datos) {
        return buscarProducto(idSucursal, idProducto)
                .flatMap(
                        actual ->
                                productos.guardar(
                                        new ProductoEntity(
                                                actual.idProducto(),
                                                actual.idSucursal(),
                                                datos.nombre().trim(),
                                                actual.stock(),
                                                actual.version())))
                .map(mapper::convertir);
    }

    @Transactional
    public Mono<Void> eliminar(Long idSucursal, Long idProducto) {
        return buscarProducto(idSucursal, idProducto).flatMap(productos::eliminar);
    }

    private Mono<ProductoEntity> buscarProducto(Long idSucursal, Long idProducto) {
        return productos
                .buscar(idProducto)
                .filter(producto -> producto.idSucursal().equals(idSucursal))
                .switchIfEmpty(
                        Mono.error(ExcepcionNegocio.noEncontrado("El producto de esta sucursal")));
    }
}

package com.moreno.gestorfranquicias.modules.productos.usecase;

import com.moreno.gestorfranquicias.crosscutting.domain.request.ActualizarNombreRequest;
import com.moreno.gestorfranquicias.crosscutting.domain.request.ActualizarStockRequest;
import com.moreno.gestorfranquicias.crosscutting.domain.request.CrearProductoRequest;
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

    /** Mono: crea un producto después de validar la sucursal. */
    @Override
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

    /** Flux: la consulta puede devolver varios productos. */
    @Override
    public Flux<ProductoResponse> listar() {
        return productos.listar().map(mapper::convertir);
    }

    /** Mono: busca un producto concreto de la sucursal. */
    @Override
    public Mono<ProductoResponse> consultar(Long idSucursal, Long idProducto) {
        return buscarProducto(idSucursal, idProducto).map(mapper::convertir);
    }

    /** Mono: actualiza el stock de un único producto. */
    @Override
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

    /** Mono: cambia el nombre de un producto. */
    @Override
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

    /** Mono<Void>: elimina el registro sin devolver un cuerpo. */
    @Override
    @Transactional
    public Mono<Void> eliminar(Long idSucursal, Long idProducto) {
        return buscarProducto(idSucursal, idProducto).flatMap(productos::eliminar);
    }

    /** Mono: comprueba que el producto pertenezca a la sucursal. */
    private Mono<ProductoEntity> buscarProducto(Long idSucursal, Long idProducto) {
        return productos
                .buscar(idProducto)
                .filter(producto -> producto.idSucursal().equals(idSucursal))
                .switchIfEmpty(
                        Mono.error(ExcepcionNegocio.noEncontrado("El producto de esta sucursal")));
    }
}

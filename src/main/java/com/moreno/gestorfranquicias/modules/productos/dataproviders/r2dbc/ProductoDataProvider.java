package com.moreno.gestorfranquicias.modules.productos.dataproviders.r2dbc;
import com.moreno.gestorfranquicias.crosscutting.persistence.producto.entity.ProductoEntity;
import com.moreno.gestorfranquicias.crosscutting.persistence.producto.repository.ProductoRepository;
import com.moreno.gestorfranquicias.crosscutting.domain.response.ProductoMayorStockResponse;
import com.moreno.gestorfranquicias.modules.productos.dataproviders.IProductoDataProvider;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
@Component
public class ProductoDataProvider implements IProductoDataProvider {
    private final ProductoRepository repositorio;
    private final DatabaseClient cliente;

    public ProductoDataProvider(ProductoRepository repositorio, DatabaseClient cliente) {
        this.repositorio = repositorio;
        this.cliente = cliente;
    }

    public Mono<ProductoEntity> buscar(Long idProducto) { return repositorio.findById(idProducto); }
    public Flux<ProductoEntity> listar() { return repositorio.findAllByOrderByIdProductoAsc(); }
    public Flux<ProductoEntity> listarPorSucursal(Long idSucursal) {
        return repositorio.findByIdSucursalOrderByIdProductoAsc(idSucursal);
    }
    public Mono<ProductoEntity> guardar(ProductoEntity producto) { return repositorio.save(producto); }
    public Mono<Void> eliminar(ProductoEntity producto) { return repositorio.delete(producto); }

    public Flux<ProductoMayorStockResponse> obtenerMayorStockPorSucursal(Long idFranquicia) {
        return cliente.sql("""
                SELECT id_sucursal, nombre_sucursal, id_producto, nombre_producto, stock
                FROM (
                    SELECT s.id_sucursal, s.nombre AS nombre_sucursal,
                           p.id_producto, p.nombre AS nombre_producto, p.stock,
                           ROW_NUMBER() OVER (
                               PARTITION BY s.id_sucursal
                               ORDER BY p.stock DESC, p.id_producto ASC
                           ) AS posicion
                    FROM sucursales s
                    INNER JOIN productos p ON p.id_sucursal = s.id_sucursal
                    WHERE s.id_franquicia = :idFranquicia
                ) ordenados
                WHERE posicion = 1
                ORDER BY id_sucursal
                """)
                .bind("idFranquicia", idFranquicia)
                .map((fila, metadata) -> new ProductoMayorStockResponse(
                    fila.get("id_sucursal", Long.class),
                    fila.get("nombre_sucursal", String.class),
                    fila.get("id_producto", Long.class),
                    fila.get("nombre_producto", String.class),
                    fila.get("stock", Integer.class)))
                .all();
    }
}

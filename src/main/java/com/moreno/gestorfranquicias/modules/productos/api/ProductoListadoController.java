package com.moreno.gestorfranquicias.modules.productos.api;

import com.moreno.gestorfranquicias.crosscutting.domain.common.ApiResponse;
import com.moreno.gestorfranquicias.crosscutting.domain.response.ProductoResponse;
import com.moreno.gestorfranquicias.modules.productos.usecase.IProductoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@Tag(name = "Productos", description = "Administración del inventario de productos por sucursal")
public class ProductoListadoController {
    private final IProductoService servicio;

    public ProductoListadoController(IProductoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/api/v1/productos")
    @Operation(
            summary = "Consultar todos los productos",
            description =
                    "Lista los productos de todas las sucursales e incluye el identificador de su sucursal")
    public Mono<ResponseEntity<ApiResponse<List<ProductoResponse>>>> listar() {
        return servicio.listar()
                .collectList()
                .map(
                        registros ->
                                ResponseEntity.ok(
                                        ApiResponse.correcto(
                                                "Productos consultados correctamente.",
                                                registros)));
    }
}

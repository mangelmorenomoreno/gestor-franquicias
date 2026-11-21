package com.moreno.gestorfranquicias.modules.productos.api;

import com.moreno.gestorfranquicias.crosscutting.domain.common.ApiResponse;
import com.moreno.gestorfranquicias.crosscutting.domain.request.*;
import com.moreno.gestorfranquicias.crosscutting.domain.response.ProductoResponse;
import com.moreno.gestorfranquicias.modules.productos.usecase.IProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/sucursales/{idSucursal}/productos")
@Tag(name = "Productos", description = "Administración del inventario de productos por sucursal")
public class ProductoController {
  private final IProductoService servicio;

  public ProductoController(IProductoService servicio) {
    this.servicio = servicio;
  }

  @PostMapping
  @Operation(summary = "Agregar un producto a una sucursal")
  public Mono<ResponseEntity<ApiResponse<ProductoResponse>>> crear(
      @PathVariable Long idSucursal, @Valid @RequestBody CrearProductoRequest datos) {
    return servicio
        .crear(idSucursal, datos)
        .map(
            respuesta ->
                ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.creado("Producto creado correctamente.", respuesta)));
  }

  @GetMapping("/{idProducto}")
  @Operation(summary = "Consultar un producto de la sucursal")
  public Mono<ResponseEntity<ApiResponse<ProductoResponse>>> consultar(
      @PathVariable Long idSucursal, @PathVariable Long idProducto) {
    return servicio
        .consultar(idSucursal, idProducto)
        .map(
            respuesta ->
                ResponseEntity.ok(
                    ApiResponse.correcto("Producto consultado correctamente.", respuesta)));
  }

  @PatchMapping("/{idProducto}/stock")
  @Operation(summary = "Actualizar la cantidad de stock de un producto")
  public Mono<ResponseEntity<ApiResponse<ProductoResponse>>> cambiarStock(
      @PathVariable Long idSucursal,
      @PathVariable Long idProducto,
      @Valid @RequestBody ActualizarStockRequest datos) {
    return servicio
        .cambiarStock(idSucursal, idProducto, datos)
        .map(
            respuesta ->
                ResponseEntity.ok(
                    ApiResponse.correcto("Stock actualizado correctamente.", respuesta)));
  }

  @PatchMapping("/{idProducto}")
  @Operation(summary = "Cambiar el nombre de un producto")
  public Mono<ResponseEntity<ApiResponse<ProductoResponse>>> cambiarNombre(
      @PathVariable Long idSucursal,
      @PathVariable Long idProducto,
      @Valid @RequestBody ActualizarNombreRequest datos) {
    return servicio
        .cambiarNombre(idSucursal, idProducto, datos)
        .map(
            respuesta ->
                ResponseEntity.ok(
                    ApiResponse.correcto("Nombre del producto actualizado.", respuesta)));
  }

  @DeleteMapping("/{idProducto}")
  @Operation(summary = "Eliminar un producto de una sucursal")
  public Mono<ResponseEntity<Void>> eliminar(
      @PathVariable Long idSucursal, @PathVariable Long idProducto) {
    return servicio.eliminar(idSucursal, idProducto).thenReturn(ResponseEntity.noContent().build());
  }
}

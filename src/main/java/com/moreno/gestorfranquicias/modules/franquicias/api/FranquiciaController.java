package com.moreno.gestorfranquicias.modules.franquicias.api;

import com.moreno.gestorfranquicias.crosscutting.domain.common.ApiResponse;
import com.moreno.gestorfranquicias.crosscutting.domain.request.*;
import com.moreno.gestorfranquicias.crosscutting.domain.response.*;
import com.moreno.gestorfranquicias.modules.franquicias.usecase.IFranquiciaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/franquicias")
@Tag(name = "Franquicias", description = "Administración de franquicias y consultas de sucursales")
public class FranquiciaController {
    private final IFranquiciaService servicio;

    public FranquiciaController(IFranquiciaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Crear una franquicia")
    public Mono<ResponseEntity<ApiResponse<FranquiciaResumenResponse>>> crear(
            @Valid @RequestBody CrearFranquiciaRequest datos) {
        return servicio.crear(datos)
                .map(
                        respuesta ->
                                ResponseEntity.status(HttpStatus.CREATED)
                                        .body(
                                                ApiResponse.creado(
                                                        "Franquicia creada correctamente.",
                                                        respuesta)));
    }

    @GetMapping
    @Operation(summary = "Consultar todas las franquicias")
    public Mono<ResponseEntity<ApiResponse<List<FranquiciaResumenResponse>>>> listar() {
        return servicio.listar()
                .collectList()
                .map(
                        registros ->
                                ResponseEntity.ok(
                                        ApiResponse.correcto(
                                                "Franquicias consultadas correctamente.",
                                                registros)));
    }

    @GetMapping("/{idFranquicia}")
    @Operation(summary = "Consultar una franquicia con sus sucursales")
    public Mono<ResponseEntity<ApiResponse<FranquiciaDetalleResponse>>> consultar(
            @PathVariable Long idFranquicia) {
        return servicio.consultar(idFranquicia)
                .map(
                        respuesta ->
                                ResponseEntity.ok(
                                        ApiResponse.correcto(
                                                "Franquicia consultada correctamente.",
                                                respuesta)));
    }

    @GetMapping("/{idFranquicia}/sucursales")
    @Operation(summary = "Consultar todas las sucursales de una franquicia")
    public Mono<ResponseEntity<ApiResponse<List<SucursalResumenResponse>>>> listarSucursales(
            @PathVariable Long idFranquicia) {
        return servicio.listarSucursales(idFranquicia)
                .collectList()
                .map(
                        registros ->
                                ResponseEntity.ok(
                                        ApiResponse.correcto(
                                                "Sucursales consultadas correctamente.",
                                                registros)));
    }

    @GetMapping("/{idFranquicia}/productos/mayor-stock")
    @Operation(summary = "Obtener el producto con mayor stock de cada sucursal")
    public Mono<ResponseEntity<ApiResponse<List<ProductoMayorStockResponse>>>> obtenerMayorStock(
            @PathVariable Long idFranquicia) {
        return servicio.obtenerMayorStock(idFranquicia)
                .collectList()
                .map(
                        registros ->
                                ResponseEntity.ok(
                                        ApiResponse.correcto(
                                                "Productos con mayor stock consultados correctamente.",
                                                registros)));
    }

    @PatchMapping("/{idFranquicia}")
    @Operation(summary = "Cambiar el nombre de una franquicia")
    public Mono<ResponseEntity<ApiResponse<FranquiciaResumenResponse>>> cambiarNombre(
            @PathVariable Long idFranquicia, @Valid @RequestBody ActualizarNombreRequest datos) {
        return servicio.cambiarNombre(idFranquicia, datos)
                .map(
                        respuesta ->
                                ResponseEntity.ok(
                                        ApiResponse.correcto(
                                                "Nombre de la franquicia actualizado.",
                                                respuesta)));
    }
}

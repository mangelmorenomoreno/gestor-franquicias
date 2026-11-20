package com.moreno.gestorfranquicias.modules.sucursales.api;
import com.moreno.gestorfranquicias.crosscutting.domain.common.ApiResponse;
import com.moreno.gestorfranquicias.crosscutting.domain.request.*;
import com.moreno.gestorfranquicias.crosscutting.domain.response.*;
import com.moreno.gestorfranquicias.modules.sucursales.usecase.ISucursalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Sucursales", description = "Administración de sucursales y sus productos")
public class SucursalController {
    private final ISucursalService servicio;
    public SucursalController(ISucursalService servicio) { this.servicio = servicio; }

    @PostMapping("/franquicias/{idFranquicia}/sucursales")
    @Operation(summary = "Agregar una sucursal a la franquicia")
    public Mono<ResponseEntity<ApiResponse<SucursalResumenResponse>>> crear(
            @PathVariable Long idFranquicia, @Valid @RequestBody CrearSucursalRequest datos) {
        return servicio.crear(idFranquicia, datos)
                .map(respuesta -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.creado("Sucursal creada correctamente.", respuesta)));
    }

    @GetMapping("/sucursales/{idSucursal}")
    @Operation(summary = "Consultar una sucursal con todos sus productos")
    public Mono<ResponseEntity<ApiResponse<SucursalDetalleResponse>>> consultar(@PathVariable Long idSucursal) {
        return servicio.consultar(idSucursal)
                .map(respuesta -> ResponseEntity.ok(ApiResponse.correcto("Sucursal consultada correctamente.", respuesta)));
    }

    @GetMapping("/sucursales/{idSucursal}/productos")
    @Operation(summary = "Consultar todos los productos de una sucursal")
    public Mono<ResponseEntity<ApiResponse<List<ProductoResponse>>>> listarProductos(@PathVariable Long idSucursal) {
        return servicio.listarProductos(idSucursal).collectList()
                .map(registros -> ResponseEntity.ok(ApiResponse.correcto("Productos consultados correctamente.", registros)));
    }

    @PatchMapping("/sucursales/{idSucursal}")
    @Operation(summary = "Cambiar el nombre de una sucursal")
    public Mono<ResponseEntity<ApiResponse<SucursalResumenResponse>>> cambiarNombre(
            @PathVariable Long idSucursal, @Valid @RequestBody ActualizarNombreRequest datos) {
        return servicio.cambiarNombre(idSucursal, datos)
                .map(respuesta -> ResponseEntity.ok(ApiResponse.correcto("Nombre de la sucursal actualizado.", respuesta)));
    }
}

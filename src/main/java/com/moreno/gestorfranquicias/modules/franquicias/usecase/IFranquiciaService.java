package com.moreno.gestorfranquicias.modules.franquicias.usecase;
import com.moreno.gestorfranquicias.crosscutting.domain.request.*;
import com.moreno.gestorfranquicias.crosscutting.domain.response.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
public interface IFranquiciaService {

Mono<FranquiciaResumenResponse> crear(CrearFranquiciaRequest datos);
Flux<FranquiciaResumenResponse> listar();
Mono<FranquiciaDetalleResponse> consultar(Long idFranquicia);
Flux<SucursalResumenResponse> listarSucursales(Long idFranquicia);
Flux<ProductoMayorStockResponse> obtenerMayorStock(Long idFranquicia);
Mono<FranquiciaResumenResponse> cambiarNombre(Long idFranquicia, ActualizarNombreRequest datos);
}

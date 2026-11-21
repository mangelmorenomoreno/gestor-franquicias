package com.moreno.gestorfranquicias.modules.franquicias.usecase;

import static org.mockito.Mockito.*;

import com.moreno.gestorfranquicias.crosscutting.domain.response.ProductoMayorStockResponse;
import com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.entity.FranquiciaEntity;
import com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.mapper.FranquiciaMapper;
import com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.entity.SucursalEntity;
import com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.mapper.SucursalMapper;
import com.moreno.gestorfranquicias.modules.franquicias.dataproviders.IFranquiciaDataProvider;
import com.moreno.gestorfranquicias.modules.productos.dataproviders.IProductoDataProvider;
import com.moreno.gestorfranquicias.modules.sucursales.dataproviders.ISucursalDataProvider;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class FranquiciaServiceTest {
  private final IFranquiciaDataProvider franquicias = mock(IFranquiciaDataProvider.class);
  private final ISucursalDataProvider sucursales = mock(ISucursalDataProvider.class);
  private final IProductoDataProvider productos = mock(IProductoDataProvider.class);
  private final FranquiciaService servicio =
      new FranquiciaService(
          franquicias, sucursales, productos, new FranquiciaMapper(), new SucursalMapper());

  @Test
  void consultarFranquiciaSinSucursalesRetornaListaVacia() {
    when(franquicias.buscar(1L)).thenReturn(Mono.just(new FranquiciaEntity(1L, "Central")));
    when(sucursales.listarPorFranquicia(1L)).thenReturn(Flux.empty());
    StepVerifier.create(servicio.consultar(1L))
        .expectNextMatches(
            respuesta -> respuesta.idFranquicia() == 1L && respuesta.sucursales().isEmpty())
        .verifyComplete();
  }

  @Test
  void consultarFranquiciaConSusSucursales() {
    when(franquicias.buscar(1L)).thenReturn(Mono.just(new FranquiciaEntity(1L, "Central")));
    when(sucursales.listarPorFranquicia(1L))
        .thenReturn(
            Flux.just(new SucursalEntity(10L, 1L, "Norte"), new SucursalEntity(11L, 1L, "Sur")));
    StepVerifier.create(servicio.consultar(1L))
        .expectNextMatches(
            respuesta ->
                respuesta.sucursales().size() == 2
                    && respuesta.sucursales().get(0).nombre().equals("Norte"))
        .verifyComplete();
  }

  @Test
  void consultaDeMayorStockDevuelveUnProductoPorSucursal() {
    when(franquicias.buscar(1L)).thenReturn(Mono.just(new FranquiciaEntity(1L, "Central")));
    when(productos.obtenerMayorStockPorSucursal(1L))
        .thenReturn(
            Flux.just(
                new ProductoMayorStockResponse(10L, "Norte", 5L, "Agua", 40),
                new ProductoMayorStockResponse(11L, "Sur", 6L, "Gaseosa", 80)));
    StepVerifier.create(servicio.obtenerMayorStock(1L)).expectNextCount(2).verifyComplete();
  }
}

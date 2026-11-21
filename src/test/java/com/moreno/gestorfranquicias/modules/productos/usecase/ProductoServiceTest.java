package com.moreno.gestorfranquicias.modules.productos.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.moreno.gestorfranquicias.crosscutting.domain.request.ActualizarStockRequest;
import com.moreno.gestorfranquicias.crosscutting.persistence.producto.entity.ProductoEntity;
import com.moreno.gestorfranquicias.crosscutting.persistence.producto.mapper.ProductoMapper;
import com.moreno.gestorfranquicias.infrastructure.exception.ExcepcionNegocio;
import com.moreno.gestorfranquicias.modules.productos.dataproviders.IProductoDataProvider;
import com.moreno.gestorfranquicias.modules.sucursales.dataproviders.ISucursalDataProvider;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ProductoServiceTest {
    @Test
    void consultarTodosLosProductosDevuelveVacioSiNoHayRegistros() {
        var productos = mock(IProductoDataProvider.class);
        when(productos.listar()).thenReturn(Flux.empty());
        var servicio =
                new ProductoService(
                        productos, mock(ISucursalDataProvider.class), new ProductoMapper());
        StepVerifier.create(servicio.listar()).verifyComplete();
        verify(productos).listar();
    }

    @Test
    void noPermiteConsultarProductoDeOtraSucursal() {
        var productos = mock(IProductoDataProvider.class);
        when(productos.buscar(5L))
                .thenReturn(Mono.just(new ProductoEntity(5L, 10L, "Agua", 20, 0L)));
        var servicio =
                new ProductoService(
                        productos, mock(ISucursalDataProvider.class), new ProductoMapper());
        StepVerifier.create(servicio.consultar(11L, 5L))
                .expectError(ExcepcionNegocio.class)
                .verify();
    }

    @Test
    void actualizarStockConservaLaVersionParaControlarConcurrencia() {
        var productos = mock(IProductoDataProvider.class);
        when(productos.buscar(5L))
                .thenReturn(Mono.just(new ProductoEntity(5L, 10L, "Agua", 20, 4L)));
        when(productos.guardar(any()))
                .thenAnswer(invocacion -> Mono.just(invocacion.getArgument(0)));
        var servicio =
                new ProductoService(
                        productos, mock(ISucursalDataProvider.class), new ProductoMapper());
        StepVerifier.create(servicio.cambiarStock(10L, 5L, new ActualizarStockRequest(50)))
                .expectNextMatches(
                        respuesta -> respuesta.stock() == 50 && respuesta.idSucursal() == 10L)
                .verifyComplete();
        verify(productos).guardar(new ProductoEntity(5L, 10L, "Agua", 50, 4L));
    }
}

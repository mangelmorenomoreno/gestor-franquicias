package com.moreno.gestorfranquicias.modules.sucursales.usecase;
import com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.entity.SucursalEntity;
import com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.mapper.SucursalMapper;
import com.moreno.gestorfranquicias.crosscutting.persistence.producto.mapper.ProductoMapper;
import com.moreno.gestorfranquicias.modules.franquicias.dataproviders.IFranquiciaDataProvider;
import com.moreno.gestorfranquicias.modules.sucursales.dataproviders.ISucursalDataProvider;
import com.moreno.gestorfranquicias.modules.productos.dataproviders.IProductoDataProvider;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.mockito.Mockito.*;

class SucursalServiceTest {
    @Test
    void consultarSucursalSinProductosDevuelveListaVacia() {
        var franquicias = mock(IFranquiciaDataProvider.class);
        var sucursales = mock(ISucursalDataProvider.class);
        var productos = mock(IProductoDataProvider.class);
        when(sucursales.buscar(5L)).thenReturn(Mono.just(new SucursalEntity(5L, 1L, "Norte")));
        when(productos.listarPorSucursal(5L)).thenReturn(Flux.empty());
        var servicio = new SucursalService(franquicias, sucursales, productos,
                new SucursalMapper(), new ProductoMapper());
        StepVerifier.create(servicio.consultar(5L))
                .expectNextMatches(respuesta -> respuesta.idFranquicia() == 1L && respuesta.productos().isEmpty())
                .verifyComplete();
    }
}

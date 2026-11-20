package com.moreno.gestorfranquicias.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class FranquiciasIntegracionTest {

    @Container
    static PostgreSQLContainer<?> baseDatos = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("gestor_franquicias")
            .withUsername("pruebas")
            .withPassword("pruebas");

    @DynamicPropertySource
    static void configurarBaseDeDatos(DynamicPropertyRegistry propiedades) {
        propiedades.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + baseDatos.getHost()
                + ":" + baseDatos.getMappedPort(5432) + "/" + baseDatos.getDatabaseName());
        propiedades.add("spring.r2dbc.username", baseDatos::getUsername);
        propiedades.add("spring.r2dbc.password", baseDatos::getPassword);
        propiedades.add("spring.flyway.url", baseDatos::getJdbcUrl);
        propiedades.add("spring.flyway.user", baseDatos::getUsername);
        propiedades.add("spring.flyway.password", baseDatos::getPassword);
    }

    @Autowired
    WebTestClient api;
    @Autowired
    ObjectMapper json;

    @Test
    void guardaRelacionesConsultaMayorStockYEliminaUnProducto() throws Exception {
        long franquicia = crear("/api/v1/franquicias", Map.of("nombre", "Central"), "idFranquicia");
        long norte = crear("/api/v1/franquicias/" + franquicia + "/sucursales",
                Map.of("nombre", "Norte"), "idSucursal");
        long sur = crear("/api/v1/franquicias/" + franquicia + "/sucursales",
                Map.of("nombre", "Sur"), "idSucursal");
        long agua = crear("/api/v1/sucursales/" + norte + "/productos",
                Map.of("nombre", "Agua", "stock", 20), "idProducto");
        crear("/api/v1/sucursales/" + norte + "/productos",
                Map.of("nombre", "Gaseosa", "stock", 40), "idProducto");
        crear("/api/v1/sucursales/" + sur + "/productos",
                Map.of("nombre", "Jugo", "stock", 70), "idProducto");

        JsonNode sucursales = consultar("/api/v1/franquicias/" + franquicia).path("data").path("sucursales");
        assertThat(sucursales.size()).isEqualTo(2);
        JsonNode productos = consultar("/api/v1/sucursales/" + norte).path("data").path("productos");
        assertThat(productos.size()).isEqualTo(2);

        JsonNode mayores = consultar("/api/v1/franquicias/" + franquicia + "/productos/mayor-stock").path("data");
        assertThat(mayores.size()).isEqualTo(2);
        assertThat(mayores.get(0).path("stock").asInt()).isEqualTo(40);
        assertThat(mayores.get(1).path("stock").asInt()).isEqualTo(70);

        api.patch().uri("/api/v1/sucursales/" + norte + "/productos/" + agua + "/stock")
                .bodyValue(Map.of("stock", 90)).exchange().expectStatus().isOk();
        assertThat(consultar("/api/v1/franquicias/" + franquicia + "/productos/mayor-stock")
                .path("data").get(0).path("stock").asInt()).isEqualTo(90);

        api.delete().uri("/api/v1/sucursales/" + norte + "/productos/" + agua)
                .exchange().expectStatus().isNoContent();
        api.get().uri("/api/v1/sucursales/" + norte + "/productos/" + agua)
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void franquiciaSinSucursalesDevuelveColeccionVacia() throws Exception {
        long franquicia = crear("/api/v1/franquicias", Map.of("nombre", "Sin sucursales"), "idFranquicia");
        assertThat(consultar("/api/v1/franquicias/" + franquicia)
                .path("data").path("sucursales").size()).isZero();
    }

    private long crear(String ruta, Map<String, ?> datos, String nombreIdentificador) throws Exception {
        String respuesta = api.post().uri(ruta).bodyValue(datos)
                .exchange().expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();
        return json.readTree(respuesta).path("data").path(nombreIdentificador).asLong();
    }

    private JsonNode consultar(String ruta) throws Exception {
        String respuesta = api.get().uri(ruta).exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        return json.readTree(respuesta);
    }
}

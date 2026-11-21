package com.moreno.gestorfranquicias.integracion;

import static org.assertj.core.api.Assertions.assertThat;

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

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class FranquiciasIntegracionTest {

    @Container
    static PostgreSQLContainer<?> baseDatos =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("gestor_franquicias")
                    .withUsername("pruebas")
                    .withPassword("pruebas");

    @DynamicPropertySource
    static void configurarBaseDeDatos(DynamicPropertyRegistry propiedades) {
        propiedades.add(
                "spring.r2dbc.url",
                () ->
                        "r2dbc:postgresql://"
                                + baseDatos.getHost()
                                + ":"
                                + baseDatos.getMappedPort(5432)
                                + "/"
                                + baseDatos.getDatabaseName());
        propiedades.add("spring.r2dbc.username", baseDatos::getUsername);
        propiedades.add("spring.r2dbc.password", baseDatos::getPassword);
        propiedades.add("spring.flyway.url", baseDatos::getJdbcUrl);
        propiedades.add("spring.flyway.user", baseDatos::getUsername);
        propiedades.add("spring.flyway.password", baseDatos::getPassword);
    }

    @Autowired WebTestClient api;
    @Autowired ObjectMapper json;

    @Test
    void guardaRelacionesConsultaMayorStockYEliminaUnProducto() throws Exception {
        long franquicia = crear("/api/v1/franquicias", Map.of("nombre", "Central"), "idFranquicia");
        long norte =
                crear(
                        "/api/v1/franquicias/" + franquicia + "/sucursales",
                        Map.of("nombre", "Norte"),
                        "idSucursal");
        long sur =
                crear(
                        "/api/v1/franquicias/" + franquicia + "/sucursales",
                        Map.of("nombre", "Sur"),
                        "idSucursal");
        long agua =
                crear(
                        "/api/v1/sucursales/" + norte + "/productos",
                        Map.of("nombre", "Agua", "stock", 20),
                        "idProducto");
        crear(
                "/api/v1/sucursales/" + norte + "/productos",
                Map.of("nombre", "Gaseosa", "stock", 40),
                "idProducto");
        crear(
                "/api/v1/sucursales/" + sur + "/productos",
                Map.of("nombre", "Jugo", "stock", 70),
                "idProducto");

        JsonNode sucursales =
                consultar("/api/v1/franquicias/" + franquicia).path("data").path("sucursales");
        assertThat(sucursales.size()).isEqualTo(2);
        JsonNode productos =
                consultar("/api/v1/sucursales/" + norte).path("data").path("productos");
        assertThat(productos.size()).isEqualTo(2);

        JsonNode mayores =
                consultar("/api/v1/franquicias/" + franquicia + "/productos/mayor-stock")
                        .path("data");
        assertThat(mayores.size()).isEqualTo(2);
        assertThat(mayores.get(0).path("stock").asInt()).isEqualTo(40);
        assertThat(mayores.get(1).path("stock").asInt()).isEqualTo(70);

        api.patch()
                .uri("/api/v1/sucursales/" + norte + "/productos/" + agua + "/stock")
                .bodyValue(Map.of("stock", 90))
                .exchange()
                .expectStatus()
                .isOk();
        assertThat(
                        consultar("/api/v1/franquicias/" + franquicia + "/productos/mayor-stock")
                                .path("data")
                                .get(0)
                                .path("stock")
                                .asInt())
                .isEqualTo(90);

        api.delete()
                .uri("/api/v1/sucursales/" + norte + "/productos/" + agua)
                .exchange()
                .expectStatus()
                .isNoContent();
        api.get()
                .uri("/api/v1/sucursales/" + norte + "/productos/" + agua)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void franquiciaSinSucursalesDevuelveColeccionVacia() throws Exception {
        long franquicia =
                crear("/api/v1/franquicias", Map.of("nombre", "Sin sucursales"), "idFranquicia");
        assertThat(
                        consultar("/api/v1/franquicias/" + franquicia)
                                .path("data")
                                .path("sucursales")
                                .size())
                .isZero();
    }

    @Test
    void sucursalSinProductosDevuelveListaVacia() throws Exception {
        long franquicia =
                crear("/api/v1/franquicias", Map.of("nombre", "Franquicia Vacia"), "idFranquicia");
        long sucursal =
                crear(
                        "/api/v1/franquicias/" + franquicia + "/sucursales",
                        Map.of("nombre", "Sin inventario"),
                        "idSucursal");
        JsonNode detalle = consultar("/api/v1/sucursales/" + sucursal).path("data");
        assertThat(detalle.path("productos").size()).isZero();
        assertThat(consultar("/api/v1/sucursales/" + sucursal + "/productos").path("data").size())
                .isZero();
    }

    @Test
    void consultasNoMezclanFranquiciasNiSucursales() throws Exception {
        long franquiciaUno =
                crear("/api/v1/franquicias", Map.of("nombre", "Franquicia Uno"), "idFranquicia");
        long franquiciaDos =
                crear("/api/v1/franquicias", Map.of("nombre", "Franquicia Dos"), "idFranquicia");
        long sucursalUno =
                crear(
                        "/api/v1/franquicias/" + franquiciaUno + "/sucursales",
                        Map.of("nombre", "Sucursal Uno"),
                        "idSucursal");
        long sucursalDos =
                crear(
                        "/api/v1/franquicias/" + franquiciaDos + "/sucursales",
                        Map.of("nombre", "Sucursal Dos"),
                        "idSucursal");
        long producto =
                crear(
                        "/api/v1/sucursales/" + sucursalUno + "/productos",
                        Map.of("nombre", "Producto exclusivo", "stock", 15),
                        "idProducto");

        assertThat(
                        consultar("/api/v1/franquicias/" + franquiciaUno)
                                .path("data")
                                .path("sucursales")
                                .size())
                .isEqualTo(1);
        assertThat(
                        consultar("/api/v1/franquicias/" + franquiciaDos)
                                .path("data")
                                .path("sucursales")
                                .get(0)
                                .path("idSucursal")
                                .asLong())
                .isEqualTo(sucursalDos);

        api.get()
                .uri("/api/v1/sucursales/" + sucursalDos + "/productos/" + producto)
                .exchange()
                .expectStatus()
                .isNotFound();
        api.patch()
                .uri("/api/v1/sucursales/" + sucursalDos + "/productos/" + producto + "/stock")
                .bodyValue(Map.of("stock", 60))
                .exchange()
                .expectStatus()
                .isNotFound();
        api.delete()
                .uri("/api/v1/sucursales/" + sucursalDos + "/productos/" + producto)
                .exchange()
                .expectStatus()
                .isNotFound();
        assertThat(
                        consultar("/api/v1/sucursales/" + sucursalUno + "/productos/" + producto)
                                .path("data")
                                .path("stock")
                                .asInt())
                .isEqualTo(15);
    }

    @Test
    void mayorStockResuelveEmpateYOmiteSucursalSinProductos() throws Exception {
        long franquicia =
                crear("/api/v1/franquicias", Map.of("nombre", "Franquicia Empate"), "idFranquicia");
        long sucursal =
                crear(
                        "/api/v1/franquicias/" + franquicia + "/sucursales",
                        Map.of("nombre", "Sucursal Empate"),
                        "idSucursal");
        crear(
                "/api/v1/franquicias/" + franquicia + "/sucursales",
                Map.of("nombre", "Sucursal Sin Productos"),
                "idSucursal");
        long primerProducto =
                crear(
                        "/api/v1/sucursales/" + sucursal + "/productos",
                        Map.of("nombre", "Primero", "stock", 50),
                        "idProducto");
        crear(
                "/api/v1/sucursales/" + sucursal + "/productos",
                Map.of("nombre", "Segundo", "stock", 50),
                "idProducto");

        JsonNode resultado =
                consultar("/api/v1/franquicias/" + franquicia + "/productos/mayor-stock")
                        .path("data");
        assertThat(resultado.size()).isEqualTo(1);
        assertThat(resultado.get(0).path("idProducto").asLong()).isEqualTo(primerProducto);
        assertThat(resultado.get(0).path("stock").asInt()).isEqualTo(50);
    }

    @Test
    void validaNombresDuplicadosStockNegativoYRecursosInexistentes() throws Exception {
        long franquicia =
                crear(
                        "/api/v1/franquicias",
                        Map.of("nombre", "Franquicia Validaciones"),
                        "idFranquicia");
        long sucursal =
                crear(
                        "/api/v1/franquicias/" + franquicia + "/sucursales",
                        Map.of("nombre", "Sucursal Validaciones"),
                        "idSucursal");

        api.post()
                .uri("/api/v1/franquicias/" + franquicia + "/sucursales")
                .bodyValue(Map.of("nombre", "sucursal validaciones"))
                .exchange()
                .expectStatus()
                .isEqualTo(409);
        api.post()
                .uri("/api/v1/sucursales/" + sucursal + "/productos")
                .bodyValue(Map.of("nombre", "Producto Negativo", "stock", -1))
                .exchange()
                .expectStatus()
                .isBadRequest();
        api.post()
                .uri("/api/v1/sucursales/" + sucursal + "/productos")
                .bodyValue(Map.of("nombre", "  ", "stock", 1))
                .exchange()
                .expectStatus()
                .isBadRequest();
        api.post()
                .uri("/api/v1/franquicias/9999999/sucursales")
                .bodyValue(Map.of("nombre", "Sin franquicia"))
                .exchange()
                .expectStatus()
                .isNotFound();
        api.get().uri("/api/v1/franquicias/9999999").exchange().expectStatus().isNotFound();
    }

    @Test
    void permiteActualizarNombresDeLosTresNiveles() throws Exception {
        long franquicia =
                crear(
                        "/api/v1/franquicias",
                        Map.of("nombre", "Franquicia Renombrar"),
                        "idFranquicia");
        long sucursal =
                crear(
                        "/api/v1/franquicias/" + franquicia + "/sucursales",
                        Map.of("nombre", "Sucursal Renombrar"),
                        "idSucursal");
        long producto =
                crear(
                        "/api/v1/sucursales/" + sucursal + "/productos",
                        Map.of("nombre", "Producto Renombrar", "stock", 5),
                        "idProducto");

        api.patch()
                .uri("/api/v1/franquicias/" + franquicia)
                .bodyValue(Map.of("nombre", "Franquicia Actualizada"))
                .exchange()
                .expectStatus()
                .isOk();
        api.patch()
                .uri("/api/v1/sucursales/" + sucursal)
                .bodyValue(Map.of("nombre", "Sucursal Actualizada"))
                .exchange()
                .expectStatus()
                .isOk();
        api.patch()
                .uri("/api/v1/sucursales/" + sucursal + "/productos/" + producto)
                .bodyValue(Map.of("nombre", "Producto Actualizado"))
                .exchange()
                .expectStatus()
                .isOk();

        assertThat(
                        consultar("/api/v1/franquicias/" + franquicia)
                                .path("data")
                                .path("nombre")
                                .asText())
                .isEqualTo("Franquicia Actualizada");
        assertThat(consultar("/api/v1/sucursales/" + sucursal).path("data").path("nombre").asText())
                .isEqualTo("Sucursal Actualizada");
        assertThat(
                        consultar("/api/v1/sucursales/" + sucursal + "/productos/" + producto)
                                .path("data")
                                .path("nombre")
                                .asText())
                .isEqualTo("Producto Actualizado");
    }

    @Test
    void consultasGlobalesIncluyenFranquiciasSucursalesYProductosDeDistintosNegocios()
            throws Exception {
        long franquiciaUno =
                crear("/api/v1/franquicias", Map.of("nombre", "Global Uno"), "idFranquicia");
        long franquiciaDos =
                crear("/api/v1/franquicias", Map.of("nombre", "Global Dos"), "idFranquicia");
        long sucursalUno =
                crear(
                        "/api/v1/franquicias/" + franquiciaUno + "/sucursales",
                        Map.of("nombre", "Global Norte"),
                        "idSucursal");
        long sucursalDos =
                crear(
                        "/api/v1/franquicias/" + franquiciaDos + "/sucursales",
                        Map.of("nombre", "Global Sur"),
                        "idSucursal");
        long productoUno =
                crear(
                        "/api/v1/sucursales/" + sucursalUno + "/productos",
                        Map.of("nombre", "Global Agua", "stock", 10),
                        "idProducto");
        long productoDos =
                crear(
                        "/api/v1/sucursales/" + sucursalDos + "/productos",
                        Map.of("nombre", "Global Jugo", "stock", 25),
                        "idProducto");

        JsonNode franquicias = consultar("/api/v1/franquicias");
        assertThat(franquicias.path("success").asBoolean()).isTrue();
        assertThat(
                        buscarPorId(franquicias.path("data"), "idFranquicia", franquiciaUno)
                                .path("nombre")
                                .asText())
                .isEqualTo("Global Uno");
        assertThat(
                        buscarPorId(franquicias.path("data"), "idFranquicia", franquiciaDos)
                                .path("nombre")
                                .asText())
                .isEqualTo("Global Dos");

        JsonNode sucursales = consultar("/api/v1/sucursales");
        assertThat(sucursales.path("success").asBoolean()).isTrue();
        assertThat(
                        buscarPorId(sucursales.path("data"), "idSucursal", sucursalUno)
                                .path("idFranquicia")
                                .asLong())
                .isEqualTo(franquiciaUno);
        assertThat(
                        buscarPorId(sucursales.path("data"), "idSucursal", sucursalDos)
                                .path("idFranquicia")
                                .asLong())
                .isEqualTo(franquiciaDos);
        validarOrdenAscendente(sucursales.path("data"), "idSucursal");

        JsonNode productos = consultar("/api/v1/productos");
        assertThat(productos.path("success").asBoolean()).isTrue();
        JsonNode agua = buscarPorId(productos.path("data"), "idProducto", productoUno);
        JsonNode jugo = buscarPorId(productos.path("data"), "idProducto", productoDos);
        assertThat(agua.path("idSucursal").asLong()).isEqualTo(sucursalUno);
        assertThat(agua.path("stock").asInt()).isEqualTo(10);
        assertThat(jugo.path("idSucursal").asLong()).isEqualTo(sucursalDos);
        assertThat(jugo.path("stock").asInt()).isEqualTo(25);
        validarOrdenAscendente(productos.path("data"), "idProducto");

        assertThat(
                        buscarPorId(
                                        consultar(
                                                        "/api/v1/franquicias/"
                                                                + franquiciaUno
                                                                + "/sucursales")
                                                .path("data"),
                                        "idSucursal",
                                        sucursalUno)
                                .path("nombre")
                                .asText())
                .isEqualTo("Global Norte");
        assertThat(
                        buscarPorId(
                                        consultar(
                                                        "/api/v1/sucursales/"
                                                                + sucursalDos
                                                                + "/productos")
                                                .path("data"),
                                        "idProducto",
                                        productoDos)
                                .path("nombre")
                                .asText())
                .isEqualTo("Global Jugo");
    }

    private JsonNode buscarPorId(JsonNode registros, String campo, long identificador) {
        assertThat(registros.isArray()).isTrue();
        for (JsonNode registro : registros) {
            if (registro.path(campo).asLong() == identificador) {
                return registro;
            }
        }
        throw new AssertionError("No aparece " + campo + "=" + identificador + " en la consulta");
    }

    private void validarOrdenAscendente(JsonNode registros, String campo) {
        long anterior = 0;
        for (JsonNode registro : registros) {
            long actual = registro.path(campo).asLong();
            assertThat(actual).isGreaterThan(anterior);
            anterior = actual;
        }
    }

    private long crear(String ruta, Map<String, ?> datos, String nombreIdentificador)
            throws Exception {
        String respuesta =
                api.post()
                        .uri(ruta)
                        .bodyValue(datos)
                        .exchange()
                        .expectStatus()
                        .isCreated()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody();
        return json.readTree(respuesta).path("data").path(nombreIdentificador).asLong();
    }

    private JsonNode consultar(String ruta) throws Exception {
        String respuesta =
                api.get()
                        .uri(ruta)
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody();
        return json.readTree(respuesta);
    }
}

package shop.orders.internal.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import shop.catalog.internal.domain.PeliculaRepository;
import shop.compartido.seguridad.JwtTokens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Crear, consultar y cancelar pedidos de extremo a extremo: HTTP, catálogo real y persistencia JDBC. */
@SpringBootTest
@AutoConfigureMockMvc
class OrdersIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @Autowired
    private PeliculaRepository peliculas;

    @Autowired
    private JdbcClient jdbc;

    private String bearer() {
        return "Bearer " + tokens.emitir(1L, "USUARIO");
    }

    private long producto(String titulo, String precio, int stock) throws Exception {
        String json = "{\"titulo\":\"" + titulo + "\",\"anio\":1979,\"genero\":\"Drama\",\"director\":\"X\","
                + "\"precio\":" + precio + ",\"stock\":" + stock + "}";
        String cuerpo = mvc.perform(post("/api/peliculas")
                        .header("Authorization", "Bearer " + tokens.emitir(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private int stockDe(long id) {
        return peliculas.findById(id).orElseThrow().getStock();
    }

    private ResultActions crear(String json) throws Exception {
        return mvc.perform(post("/api/orders").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String pedido(long... productoYCantidad) {
        StringBuilder lineas = new StringBuilder();
        for (int i = 0; i < productoYCantidad.length; i += 2) {
            lineas.append(i > 0 ? "," : "").append("{\"productId\":").append(productoYCantidad[i])
                    .append(",\"quantity\":").append(productoYCantidad[i + 1]).append("}");
        }
        return "{\"userId\":1,\"lines\":[" + lineas + "]}";
    }

    private long idDe(ResultActions respuesta) throws Exception {
        return ((Number) JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void crearConsultarYCancelarReservaYLiberaElStockReal() throws Exception {
        long alien = producto("Orders Alien", "12.99", 5);
        long matrix = producto("Orders Matrix", "7.50", 2);

        ResultActions creado = crear(pedido(alien, 2, matrix, 1)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.total").value(33.48));
        long id = idDe(creado);
        assertEquals(3, stockDe(alien));
        assertEquals(1, stockDe(matrix));

        mvc.perform(get("/api/orders/" + id).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[0].title").value("Orders Alien"))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(12.99));

        mvc.perform(post("/api/orders/" + id + "/cancel").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(5, stockDe(alien));
        assertEquals(2, stockDe(matrix));

        mvc.perform(post("/api/orders/" + id + "/cancel").header("Authorization", bearer()))
                .andExpect(status().isConflict());
        assertEquals(5, stockDe(alien));
    }

    @Test
    void unPedidoConUnaLineaSinStockDa409YNoDejaNadaReservadoNiGuardado() throws Exception {
        long alien = producto("Orders Alien 2", "12.99", 5);
        long matrix = producto("Orders Matrix 2", "7.50", 1);
        int antes = jdbc.sql("select count(*) from orders").query(Integer.class).single();

        crear(pedido(alien, 2, matrix, 2)).andExpect(status().isConflict());

        assertEquals(5, stockDe(alien));
        assertEquals(1, stockDe(matrix));
        assertEquals(antes, jdbc.sql("select count(*) from orders").query(Integer.class).single());
    }

    @Test
    void productoDesconocidoDa404YPedidoInexistenteDa404() throws Exception {
        crear(pedido(987654, 1)).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/987654").header("Authorization", bearer())).andExpect(status().isNotFound());
        mvc.perform(post("/api/orders/987654/cancel").header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void peticionInvalidaDa400YSinTokenDa401() throws Exception {
        crear("{\"userId\":1,\"lines\":[]}").andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(pedido(1, 1)))
                .andExpect(status().isUnauthorized());
    }
}

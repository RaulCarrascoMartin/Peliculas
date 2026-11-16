package shop.orders.api.http.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import shop.shared.security.JwtTokens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Create, get and cancel orders end to end: HTTP, real catalog and JDBC persistence. */
@SpringBootTest
@AutoConfigureMockMvc
class OrderIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @Autowired
    private JdbcClient jdbc;

    private String bearer() {
        return "Bearer " + tokens.issueToken(1L, "USUARIO");
    }

    private long product(String title, String price, int stock) throws Exception {
        String json = "{\"titulo\":\"" + title + "\",\"anio\":1979,\"genero\":\"Drama\",\"director\":\"X\","
                + "\"precio\":" + price + ",\"stock\":" + stock + "}";
        String body = mvc.perform(post("/api/peliculas")
                        .header("Authorization", "Bearer " + tokens.issueToken(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private int stockOf(long id) {
        return jdbc.sql("select stock from peliculas where id = :id").param("id", id).query(Integer.class).single();
    }

    private ResultActions create(String json) throws Exception {
        return mvc.perform(post("/api/orders").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String quantities(long... productAndQuantity) {
        StringBuilder lines = new StringBuilder();
        for (int i = 0; i < productAndQuantity.length; i += 2) {
            lines.append(i > 0 ? "," : "").append("{\"productId\":").append(productAndQuantity[i])
                    .append(",\"quantity\":").append(productAndQuantity[i + 1]).append("}");
        }
        return "{\"lines\":[" + lines + "]}";
    }

    private long idOf(ResultActions response) throws Exception {
        return ((Number) JsonPath.read(response.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void createGetAndCancelReserveAndReleaseRealStock() throws Exception {
        long alien = product("Orders Alien", "12.99", 5);
        long matrix = product("Orders Matrix", "7.50", 2);

        ResultActions created = create(quantities(alien, 2, matrix, 1)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.total").value(33.48));
        long id = idOf(created);
        assertEquals(3, stockOf(alien));
        assertEquals(1, stockOf(matrix));

        mvc.perform(get("/api/orders/" + id).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[0].title").value("Orders Alien"))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(12.99));

        mvc.perform(post("/api/orders/" + id + "/cancel").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(5, stockOf(alien));
        assertEquals(2, stockOf(matrix));

        mvc.perform(post("/api/orders/" + id + "/cancel").header("Authorization", bearer()))
                .andExpect(status().isConflict());
        assertEquals(5, stockOf(alien));
    }

    @Test
    void orderWithOutOfStockLineGives409AndLeavesNothingReservedOrSaved() throws Exception {
        long alien = product("Orders Alien 2", "12.99", 5);
        long matrix = product("Orders Matrix 2", "7.50", 1);
        int antes = jdbc.sql("select count(*) from orders").query(Integer.class).single();

        create(quantities(alien, 2, matrix, 2)).andExpect(status().isConflict());

        assertEquals(5, stockOf(alien));
        assertEquals(1, stockOf(matrix));
        assertEquals(antes, jdbc.sql("select count(*) from orders").query(Integer.class).single());
    }

    @Test
    void unknownProductGives404AndUnknownOrderGives404() throws Exception {
        create(quantities(987654, 1)).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/987654").header("Authorization", bearer())).andExpect(status().isNotFound());
        mvc.perform(post("/api/orders/987654/cancel").header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidRequestGives400AndNoTokenGives401() throws Exception {
        create("{\"lines\":[]}").andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(quantities(1, 1)))
                .andExpect(status().isUnauthorized());
    }
}

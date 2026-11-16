package shop.purchase.api.http.controller;

import com.jayway.jsonpath.JsonPath;
import shop.shared.security.JwtTokens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PurchaseControllerIntegrationTest {

    private static final String ALIEN = """
            {"titulo":"Alien","anio":1979,"genero":"Science fiction","director":"Ridley Scott","precio":12.99}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @Autowired
    private JdbcClient jdbc;

    private final List<Long> createdMovies = new ArrayList<>();
    private final List<Long> usedUsers = new ArrayList<>();

    /** The DB is shared between tests: the purchases of the used users, their carts and the movies are deleted. */
    @AfterEach
    void clean() throws Exception {
        for (long userId : usedUsers) {
            jdbc.sql("delete from lineas_compra where compra_id in (select id from compras where usuario_id = ?)")
                    .param(userId).update();
            jdbc.sql("delete from compras where usuario_id = ?").param(userId).update();
            mvc.perform(delete("/api/carrito").header("Authorization", bearer(userId)));
        }
        for (long id : createdMovies) {
            mvc.perform(delete("/api/peliculas/" + id).header("Authorization", bearerAdmin()));
        }
    }

    private String bearer(long userId) {
        if (!usedUsers.contains(userId)) {
            usedUsers.add(userId);
        }
        return "Bearer " + tokens.issueToken(userId, "USUARIO");
    }

    private String bearerAdmin() {
        return "Bearer " + tokens.issueToken(1L, "ADMIN");
    }

    private long createMovie() throws Exception {
        String body = mvc.perform(post("/api/peliculas").header("Authorization", bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(ALIEN))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        createdMovies.add(id);
        return id;
    }

    private void addToCart(long userId, long movieId) throws Exception {
        mvc.perform(post("/api/carrito/lineas").header("Authorization", bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"peliculaId\":" + movieId + "}"))
                .andExpect(status().isCreated());
    }

    private ResultActions complete(long userId) throws Exception {
        return mvc.perform(post("/api/compras").header("Authorization", bearer(userId)));
    }

    private ResultActions detail(long userId, long purchaseId) throws Exception {
        return mvc.perform(get("/api/compras/" + purchaseId).header("Authorization", bearer(userId)));
    }

    private long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void completeReturns201WithSnapshotAndClearsCart() throws Exception {
        long movie = createMovie();
        addToCart(3001, movie);

        complete(3001)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("COMPRADA"))
                .andExpect(jsonPath("$.fecha", containsString("T")))
                .andExpect(jsonPath("$.lineas", hasSize(1)))
                .andExpect(jsonPath("$.lineas[0].peliculaId").value(movie))
                .andExpect(jsonPath("$.lineas[0].titulo").value("Alien"))
                .andExpect(jsonPath("$.lineas[0].precioUnitario").value(12.99))
                .andExpect(jsonPath("$.total").value(12.99));

        mvc.perform(get("/api/carrito").header("Authorization", bearer(3001)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas").isEmpty());
    }

    @Test
    void purchasePriceDoesNotChangeIfMovieIsUpdatedLater() throws Exception {
        long movie = createMovie();
        addToCart(3002, movie);
        long purchase = idOf(complete(3002).andExpect(status().isCreated()));

        String updated = """
                {"titulo":"Alien 2","anio":1979,"genero":"Science fiction","director":"Ridley Scott","precio":99.00}""";
        mvc.perform(put("/api/peliculas/" + movie).header("Authorization", bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(updated))
                .andExpect(status().isOk());

        detail(3002, purchase)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas[0].titulo").value("Alien"))
                .andExpect(jsonPath("$.lineas[0].precioUnitario").value(12.99))
                .andExpect(jsonPath("$.total").value(12.99));
    }

    @Test
    void completeWithEmptyCartReturns409WithErrorBody() throws Exception {
        complete(3003)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void othersOrUnknownPurchaseReturns404NeverTheBodyNor403() throws Exception {
        long movie = createMovie();
        addToCart(3004, movie);
        long purchase = idOf(complete(3004).andExpect(status().isCreated()));

        detail(3005, purchase)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").isNotEmpty());
        detail(3004, 99999999L).andExpect(status().isNotFound());
    }

    @Test
    void listReturnsOnlyOwnPurchasesNewestFirst() throws Exception {
        long movie = createMovie();
        addToCart(3006, movie);
        long first = idOf(complete(3006).andExpect(status().isCreated()));
        Thread.sleep(10);
        addToCart(3006, movie);
        long second = idOf(complete(3006).andExpect(status().isCreated()));
        addToCart(3007, movie);
        complete(3007).andExpect(status().isCreated());

        mvc.perform(get("/api/compras").header("Authorization", bearer(3006)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[1].id").value(first))
                .andExpect(jsonPath("$[0].lineas[0].titulo").value("Alien"));
    }

    @Test
    void withoutTokenAllThreeRoutesReturn401() throws Exception {
        mvc.perform(post("/api/compras")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/compras")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/compras/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void evenAdminCannotSeeAnotherUsersPurchase() throws Exception {
        long movie = createMovie();
        addToCart(3008, movie);
        long purchase = idOf(complete(3008).andExpect(status().isCreated()));

        mvc.perform(get("/api/compras/" + purchase).header("Authorization", bearerAdmin()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/compras").header("Authorization", bearerAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + purchase + ")]").isEmpty());
    }

    @Test
    void userIdInRequestIsIgnoredAndTokenOneIsUsed() throws Exception {
        long movie = createMovie();
        addToCart(3009, movie);

        mvc.perform(post("/api/compras?usuarioId=3009").header("Authorization", bearer(3010)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/compras?usuarioId=3009").header("Authorization", bearer(3010)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/carrito").header("Authorization", bearer(3009)))
                .andExpect(jsonPath("$.lineas", hasSize(1)));
    }
}

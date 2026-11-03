package shop.cart.api.http.controller;

import com.jayway.jsonpath.JsonPath;
import shop.shared.security.JwtTokens;
import shop.catalog.internal.domain.model.Movie;
import shop.catalog.internal.domain.repository.MovieRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CartControllerIntegrationTest {

    private static final String ALIEN = """
            {"titulo":"Alien","anio":1979,"genero":"Science fiction","director":"Ridley Scott","precio":12.99}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @Autowired
    private MovieRepository movieRepository;

    private final List<Long> createdMovies = new ArrayList<>();

    /** The DB is shared between tests: carts are emptied and the used movies deleted. */
    @AfterEach
    void clean() throws Exception {
        for (long userId : usedUsers) {
            mvc.perform(delete("/api/carrito").header("Authorization", bearer(userId, "USUARIO")));
        }
        for (long id : createdMovies) {
            mvc.perform(delete("/api/peliculas/" + id).header("Authorization", bearer(1, "ADMIN")));
        }
    }

    private final List<Long> usedUsers = new ArrayList<>();

    private String bearer(long userId, String role) {
        if (!usedUsers.contains(userId) && role.equals("USUARIO")) {
            usedUsers.add(userId);
        }
        return "Bearer " + tokens.issueToken(userId, role);
    }

    private long createMovie() throws Exception {
        String body = mvc.perform(post("/api/peliculas").header("Authorization", bearer(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(ALIEN))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        createdMovies.add(id);
        return id;
    }

    private long createMovieWithoutPrice() {
        Movie withoutPrice = new Movie();
        withoutPrice.setTitle("Silent");
        Movie saved = movieRepository.save(withoutPrice);
        createdMovies.add(saved.getId());
        return saved.getId();
    }

    private ResultActions add(long userId, long movieId) throws Exception {
        return mvc.perform(post("/api/carrito/lineas")
                .header("Authorization", bearer(userId, "USUARIO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"peliculaId\":" + movieId + "}"));
    }

    private ResultActions request(String ruta) throws Exception {
        return mvc.perform(get(ruta));
    }

    private ResultActions requestWithToken(long userId, String ruta) throws Exception {
        return mvc.perform(get(ruta).header("Authorization", bearer(userId, "USUARIO")));
    }

    private ResultActions remove(long userId, long movieId) throws Exception {
        return removeWithValue(userId, "/api/carrito/lineas/" + movieId);
    }

    private ResultActions removeWithValue(long userId, String ruta) throws Exception {
        return mvc.perform(delete(ruta).header("Authorization", bearer(userId, "USUARIO")));
    }

    @Test
    void cartOfUserWithoutCartIsEmptyWithZeroTotal() throws Exception {
        requestWithToken(2001, "/api/carrito")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void addReturns201AndViewShowsLivePriceAndTotal() throws Exception {
        long movie = createMovie();

        add(2002, movie)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.peliculaId").value(movie))
                .andExpect(jsonPath("$.titulo").value("Alien"))
                .andExpect(jsonPath("$.precio").value(12.99));

        requestWithToken(2002, "/api/carrito")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.lineas[0].peliculaId").value(movie))
                .andExpect(jsonPath("$.lineas[0].precio").value(12.99))
                .andExpect(jsonPath("$.total").value(12.99));

        String updated = """
                {"titulo":"Alien","anio":1979,"genero":"Science fiction","director":"Ridley Scott","precio":15.50}""";
        mvc.perform(put("/api/peliculas/" + movie).header("Authorization", bearer(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(updated))
                .andExpect(status().isOk());

        requestWithToken(2002, "/api/carrito")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas[0].precio").value(15.50))
                .andExpect(jsonPath("$.total").value(15.50));
    }

    @Test
    void addingSameMovieTwiceReturns409WithErrorBody() throws Exception {
        long movie = createMovie();
        add(2003, movie).andExpect(status().isCreated());

        add(2003, movie)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.errores").isMap());

        long other = createMovie();
        add(2003, other).andExpect(status().isCreated());
    }

    @Test
    void addingUnknownMovieReturns404() throws Exception {
        add(2004, 99999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void addingMovieWithoutPriceReturns409() throws Exception {
        long movie = createMovieWithoutPrice();

        add(2005, movie).andExpect(status().isConflict());
    }

    @Test
    void bodyWithoutValidIdReturns400() throws Exception {
        mvc.perform(post("/api/carrito/lineas").header("Authorization", bearer(2006, "USUARIO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.errores.peliculaId").isNotEmpty());

        mvc.perform(post("/api/carrito/lineas").header("Authorization", bearer(2006, "USUARIO"))
                        .contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removingLineReturns204AndCartIsEmpty() throws Exception {
        long movie = createMovie();
        add(2007, movie).andExpect(status().isCreated());

        remove(2007, movie).andExpect(status().isNoContent());

        requestWithToken(2007, "/api/carrito")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void removingMovieNotInCartReturns404() throws Exception {
        remove(2008, 99999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404));
    }

    @Test
    void clearingCartReturns204AndIsIdempotent() throws Exception {
        long first = createMovie();
        long second = createMovie();
        add(2009, first).andExpect(status().isCreated());
        add(2009, second).andExpect(status().isCreated());

        removeWithValue(2009, "/api/carrito").andExpect(status().isNoContent());

        requestWithToken(2009, "/api/carrito").andExpect(jsonPath("$.lineas").isEmpty());

        removeWithValue(2009, "/api/carrito").andExpect(status().isNoContent());
    }

    @Test
    void eachUserSeesOnlyTheirCart() throws Exception {
        long first = createMovie();
        long second = createMovie();
        add(2010, first).andExpect(status().isCreated());

        requestWithToken(2011, "/api/carrito")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas").isEmpty());

        add(2011, second).andExpect(status().isCreated());

        requestWithToken(2010, "/api/carrito")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.lineas[0].peliculaId").value(first));
    }

    @Test
    void deletingMovieRemovesItsLinesFromCarts() throws Exception {
        long movie = createMovie();
        add(2012, movie).andExpect(status().isCreated());

        mvc.perform(delete("/api/peliculas/" + movie).header("Authorization", bearer(1, "ADMIN")))
                .andExpect(status().isNoContent());
        createdMovies.remove((Long) movie);

        requestWithToken(2012, "/api/carrito")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas").isEmpty());
    }

    @Test
    void withoutTokenAllCartRoutesReturn401() throws Exception {
        request("/api/carrito").andExpect(status().isUnauthorized());
        mvc.perform(post("/api/carrito/lineas")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"peliculaId\":1}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/carrito/lineas/1")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/carrito")).andExpect(status().isUnauthorized());
    }

    @Test
    void userIdInBodyIsIgnoredAndLineGoesToTokenUsersCart() throws Exception {
        long movie = createMovie();

        mvc.perform(post("/api/carrito/lineas").header("Authorization", bearer(2013, "USUARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"peliculaId\":" + movie + ",\"usuarioId\":2014}"))
                .andExpect(status().isCreated());

        requestWithToken(2013, "/api/carrito").andExpect(jsonPath("$.lineas", org.hamcrest.Matchers.hasSize(1)));
        requestWithToken(2014, "/api/carrito").andExpect(jsonPath("$.lineas").isEmpty());
    }
}

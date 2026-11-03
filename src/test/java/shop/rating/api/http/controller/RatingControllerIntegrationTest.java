package shop.rating.api.http.controller;

import com.jayway.jsonpath.JsonPath;
import shop.shared.security.JwtTokens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
class RatingControllerIntegrationTest {

    private static final String ALIEN = """
            {"titulo":"Alien","anio":1979,"genero":"Science fiction","director":"Ridley Scott","precio":12.99}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    private final List<Long> createdMovies = new ArrayList<>();

    /** The database is shared between tests: movies are deleted so as not to alter other listings. */
    @AfterEach
    void clean() throws Exception {
        for (long id : createdMovies) {
            mvc.perform(delete("/api/peliculas/" + id).header("Authorization", bearer(1, "ADMIN")));
        }
    }

    private String bearer(long userId, String role) {
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

    private ResultActions rate(long movieId, String autorizacion, String json) throws Exception {
        MockHttpServletRequestBuilder request = put("/api/peliculas/" + movieId + "/valoracion")
                .contentType(MediaType.APPLICATION_JSON).content(json);
        if (autorizacion != null) {
            request.header("Authorization", autorizacion);
        }
        return mvc.perform(request);
    }

    @Test
    void userRatesMovieAndReceivesTheirRating() throws Exception {
        long movie = createMovie();

        rate(movie, bearer(100, "USUARIO"), "{\"puntuacion\":4,\"comentario\":\"Very good\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.peliculaId").value(movie))
                .andExpect(jsonPath("$.puntuacion").value(4))
                .andExpect(jsonPath("$.comentario").value("Very good"));
    }

    @Test
    void ratingAgainUpdatesRatingAndAverageShowsInDetail() throws Exception {
        long movie = createMovie();
        rate(movie, bearer(101, "USUARIO"), "{\"puntuacion\":5}").andExpect(status().isOk());
        String first = rate(movie, bearer(102, "USUARIO"), "{\"puntuacion\":1}")
                .andReturn().getResponse().getContentAsString();

        rate(movie, bearer(102, "USUARIO"), "{\"puntuacion\":3,\"comentario\":\"Mejor\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(((Number) JsonPath.read(first, "$.id")).longValue()))
                .andExpect(jsonPath("$.puntuacion").value(3));

        mvc.perform(get("/api/peliculas/" + movie))
                .andExpect(jsonPath("$.valoracionMedia").value(4.0));
    }

    @Test
    void outOfRangeOrMissingScoreReturns400() throws Exception {
        long movie = createMovie();

        rate(movie, bearer(103, "USUARIO"), "{\"puntuacion\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.puntuacion").isNotEmpty());
        rate(movie, bearer(103, "USUARIO"), "{\"puntuacion\":6}").andExpect(status().isBadRequest());
        rate(movie, bearer(103, "USUARIO"), "{\"comentario\":\"no note\"}")
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/peliculas/" + movie)).andExpect(jsonPath("$.valoracionMedia").isEmpty());
    }

    @Test
    void ratingUnknownMovieReturns404() throws Exception {
        rate(987654, bearer(104, "USUARIO"), "{\"puntuacion\":3}").andExpect(status().isNotFound());
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        long movie = createMovie();

        rate(movie, null, "{\"puntuacion\":3}").andExpect(status().isUnauthorized());
    }
}

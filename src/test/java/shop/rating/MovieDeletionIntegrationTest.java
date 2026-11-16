package shop.rating;

import com.jayway.jsonpath.JsonPath;
import shop.shared.security.JwtTokens;
import shop.rating.internal.domain.repository.MovieMarkRepository;
import shop.rating.internal.domain.repository.RatingRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deleting a movie does not touch other movies' data. Without events between modules, its ratings and marks are not
 * deleted (there are no movie↔rating cycles), but the lists no longer show it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MovieDeletionIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @Autowired
    private RatingRepository ratings;

    @Autowired
    private MovieMarkRepository marks;

    private final List<Long> createdMovies = new ArrayList<>();

    @AfterEach
    void clean() throws Exception {
        for (long id : createdMovies) {
            mvc.perform(delete("/api/peliculas/" + id).header("Authorization", bearer(1, "ADMIN")));
        }
    }

    private String bearer(long userId, String role) {
        return "Bearer " + tokens.issueToken(userId, role);
    }

    private long createMovie(String title) throws Exception {
        String json = "{\"titulo\":\"" + title + "\",\"anio\":1979,\"genero\":\"Drama\",\"director\":\"X\",\"precio\":12.99}";
        String body = mvc.perform(post("/api/peliculas").header("Authorization", bearer(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        createdMovies.add(id);
        return id;
    }

    private ResultActions rate(long movieId, long userId, int score) throws Exception {
        return mvc.perform(put("/api/peliculas/" + movieId + "/valoracion")
                .header("Authorization", bearer(userId, "USUARIO"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"puntuacion\":" + score + "}"));
    }

    private ResultActions mark(long movieId, long userId, String listName) throws Exception {
        return mvc.perform(put("/api/peliculas/" + movieId + "/" + listName)
                .header("Authorization", bearer(userId, "USUARIO")));
    }

    @Test
    void deletedMovieStopsAppearingInListsEvenIfMarksRemain() throws Exception {
        long id = createMovie("Deleted One");
        mark(id, 300, "favorita").andExpect(status().isNoContent());
        mark(id, 300, "vista").andExpect(status().isNoContent());
        mvc.perform(get("/api/usuarios/me/favoritas").header("Authorization", bearer(300, "USUARIO")))
                .andExpect(jsonPath("$[0].id").value(id));

        mvc.perform(delete("/api/peliculas/" + id).header("Authorization", bearer(1, "ADMIN")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/peliculas/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/usuarios/me/favoritas").header("Authorization", bearer(300, "USUARIO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/usuarios/me/vistas").header("Authorization", bearer(300, "USUARIO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void deletingMovieDoesNotTouchOthersData() throws Exception {
        long deleted = createMovie("Deleted Two");
        long preserved = createMovie("Deleted Three");
        rate(deleted, 302, 5).andExpect(status().isOk());
        rate(preserved, 302, 3).andExpect(status().isOk());
        mark(deleted, 302, "favorita").andExpect(status().isNoContent());
        mark(preserved, 302, "favorita").andExpect(status().isNoContent());

        mvc.perform(delete("/api/peliculas/" + deleted).header("Authorization", bearer(1, "ADMIN")))
                .andExpect(status().isNoContent());

        assertEquals(1, ratings.findByMovieId(preserved).size());
        var favorites = marks.findByUserIdAndType(302L, shop.rating.internal.domain.model.MarkType.FAVORITA);
        assertTrue(favorites.stream().anyMatch(m -> m.movieId().equals(preserved)));
        mvc.perform(get("/api/usuarios/me/favoritas").header("Authorization", bearer(302, "USUARIO")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(preserved));
    }

    @Test
    void deletingUnknownMovieReturns404AndTouchesNothing() throws Exception {
        long id = createMovie("Deleted Four");
        rate(id, 303, 4).andExpect(status().isOk());

        mvc.perform(delete("/api/peliculas/987654").header("Authorization", bearer(1, "ADMIN")))
                .andExpect(status().isNotFound());

        assertEquals(1, ratings.findByMovieId(id).size());
    }
}

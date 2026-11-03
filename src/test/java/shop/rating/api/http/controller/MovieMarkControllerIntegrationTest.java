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
class MovieMarkControllerIntegrationTest {

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

    private long createMovie(String title) throws Exception {
        String json = "{\"titulo\":\"" + title + "\",\"anio\":1979,\"genero\":\"Drama\",\"director\":\"X\",\"precio\":12.99}";
        String body = mvc.perform(post("/api/peliculas").header("Authorization", bearer(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        createdMovies.add(id);
        return id;
    }

    private ResultActions mark(String ruta, long movieId, long userId) throws Exception {
        return mvc.perform(put("/api/peliculas/" + movieId + "/" + ruta).header("Authorization", bearer(userId, "USUARIO")));
    }

    private ResultActions unmark(String ruta, long movieId, long userId) throws Exception {
        return mvc.perform(delete("/api/peliculas/" + movieId + "/" + ruta).header("Authorization", bearer(userId, "USUARIO")));
    }

    private ResultActions listMarked(String ruta, long userId) throws Exception {
        return mvc.perform(get("/api/usuarios/me/" + ruta).header("Authorization", bearer(userId, "USUARIO")));
    }

    @Test
    void markingAsFavoriteAddsItToMyFavoritesWithItsData() throws Exception {
        long id = createMovie("Mark One");

        mark("favorita", id, 200).andExpect(status().isNoContent());

        listMarked("favoritas", 200)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].titulo").value("Mark One"))
                .andExpect(jsonPath("$[0].valoracionMedia").isEmpty());
        listMarked("vistas", 200).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void markingAsWatchedAndUnmarkingRemovesItFromList() throws Exception {
        long id = createMovie("Mark Two");
        mark("vista", id, 201).andExpect(status().isNoContent());
        mark("vista", id, 201).andExpect(status().isNoContent());
        listMarked("vistas", 201).andExpect(jsonPath("$.length()").value(1));

        unmark("vista", id, 201).andExpect(status().isNoContent());

        listMarked("vistas", 201).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void eachUserSeesOnlyTheirOwnLists() throws Exception {
        long id = createMovie("Mark Three");
        mark("favorita", id, 202).andExpect(status().isNoContent());

        listMarked("favoritas", 203).andExpect(jsonPath("$.length()").value(0));
        unmark("favorita", id, 203).andExpect(status().isNoContent());
        listMarked("favoritas", 202).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void markingUnknownMovieReturns404() throws Exception {
        mark("favorita", 987654, 204).andExpect(status().isNotFound());
        mark("vista", 987654, 204).andExpect(status().isNotFound());
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        mvc.perform(put("/api/peliculas/1/favorita")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/peliculas/1/vista")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me/favoritas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me/vistas")).andExpect(status().isUnauthorized());
    }

    @Test
    void unmarkWithoutPriorMarkReturns204() throws Exception {
        long id = createMovie("Mark Four");

        unmark("favorita", id, 205).andExpect(status().isNoContent());
        listMarked("favoritas", 205).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void movieCanBeInFavoritesAndWatchedAtOnce() throws Exception {
        long id = createMovie("Mark Five");

        mark("favorita", id, 206).andExpect(status().isNoContent());
        mark("vista", id, 206).andExpect(status().isNoContent());
        unmark("favorita", id, 206).andExpect(status().isNoContent());

        listMarked("favoritas", 206).andExpect(jsonPath("$.length()").value(0));
        listMarked("vistas", 206).andExpect(jsonPath("$.length()").value(1));
    }
}

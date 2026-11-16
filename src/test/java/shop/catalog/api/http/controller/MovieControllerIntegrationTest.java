package shop.catalog.api.http.controller;

import com.jayway.jsonpath.JsonPath;
import shop.shared.security.JwtTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MovieControllerIntegrationTest {

    private static final String ALIEN = """
            {"titulo":"Alien","anio":1979,"genero":"Science fiction","director":"Ridley Scott","precio":12.99}""";
    private static final String ALIENS = """
            {"titulo":"Aliens","anio":1986,"genero":"Action","director":"James Cameron","precio":9.99}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @Autowired
    private shop.catalog.internal.domain.repository.MovieRepository repo;

    private String admin() {
        return "Bearer " + tokens.issueToken(1L, "ADMIN");
    }

    private String user() {
        return "Bearer " + tokens.issueToken(2L, "USUARIO");
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 String autorizacion, String json) throws Exception {
        if (autorizacion != null) {
            request.header("Authorization", autorizacion);
        }
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json == null ? "" : json));
    }

    private long createAsAdmin(String json) throws Exception {
        String body = send(post("/api/peliculas"), admin(), json)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void adminCreatesMovieAndReceivesItsId() throws Exception {
        send(post("/api/peliculas"), admin(), ALIEN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titulo").value("Alien"))
                .andExpect(jsonPath("$.anio").value(1979))
                .andExpect(jsonPath("$.genero").value("Science fiction"))
                .andExpect(jsonPath("$.director").value("Ridley Scott"))
                .andExpect(jsonPath("$.valoracionMedia").isEmpty());
    }

    @Test
    void withoutTitleOrWithInvalidYearReturns400() throws Exception {
        send(post("/api/peliculas"), admin(),
                "{\"titulo\":\"\",\"anio\":5,\"genero\":\"Drama\",\"director\":\"X\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.titulo").isNotEmpty())
                .andExpect(jsonPath("$.errores.anio").isNotEmpty());
    }

    @Test
    void withoutGenreOrDirectorReturns400() throws Exception {
        send(post("/api/peliculas"), admin(), "{\"titulo\":\"Alien\",\"anio\":1979}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.genero").isNotEmpty())
                .andExpect(jsonPath("$.errores.director").isNotEmpty());
    }

    @Test
    void detailIsPublicAndShowsAverageRating() throws Exception {
        long id = createAsAdmin(ALIEN);

        mvc.perform(get("/api/peliculas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Alien"))
                .andExpect(jsonPath("$.valoracionMedia").isEmpty());
    }

    @Test
    void detailOfUnknownMovieReturns404() throws Exception {
        mvc.perform(get("/api/peliculas/987654"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404));
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mvc.perform(get("/api/peliculas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));
    }

    @Test
    void adminUpdatesMovie() throws Exception {
        long id = createAsAdmin(ALIEN);

        send(put("/api/peliculas/" + id), admin(), ALIENS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Aliens"))
                .andExpect(jsonPath("$.anio").value(1986));
    }

    @Test
    void updatingUnknownMovieReturns404() throws Exception {
        send(put("/api/peliculas/987654"), admin(), ALIENS).andExpect(status().isNotFound());
    }

    @Test
    void updateWithInvalidDataReturns400() throws Exception {
        long id = createAsAdmin(ALIEN);

        send(put("/api/peliculas/" + id), admin(), "{\"titulo\":\"\"}").andExpect(status().isBadRequest());
    }

    @Test
    void adminDeletesMovie() throws Exception {
        long id = createAsAdmin(ALIEN);

        send(delete("/api/peliculas/" + id), admin(), null).andExpect(status().isNoContent());

        mvc.perform(get("/api/peliculas/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void deletingUnknownMovieReturns404() throws Exception {
        send(delete("/api/peliculas/987654"), admin(), null).andExpect(status().isNotFound());
    }

    @Test
    void nonAdminUserCannotCreateUpdateNorDelete() throws Exception {
        long id = createAsAdmin(ALIEN);

        send(post("/api/peliculas"), user(), ALIEN).andExpect(status().isForbidden());
        send(put("/api/peliculas/" + id), user(), ALIENS).andExpect(status().isForbidden());
        send(delete("/api/peliculas/" + id), user(), null).andExpect(status().isForbidden());

        mvc.perform(get("/api/peliculas/" + id)).andExpect(jsonPath("$.titulo").value("Alien"));
    }

    @Test
    void withoutTokenCannotCreateUpdateNorDelete() throws Exception {
        send(post("/api/peliculas"), null, ALIEN).andExpect(status().isUnauthorized());
        send(put("/api/peliculas/1"), null, ALIENS).andExpect(status().isUnauthorized());
        send(delete("/api/peliculas/1"), null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void validatesYearBoundaries() throws Exception {
        send(post("/api/peliculas"), admin(), withYear(1887)).andExpect(status().isBadRequest());
        send(post("/api/peliculas"), admin(), withYear(2101)).andExpect(status().isBadRequest());
        send(post("/api/peliculas"), admin(), withYear(1888)).andExpect(status().isCreated());
        send(post("/api/peliculas"), admin(), withYear(2100)).andExpect(status().isCreated());
    }

    @Test
    void withoutYearReturns400() throws Exception {
        send(post("/api/peliculas"), admin(), "{\"titulo\":\"Alien\",\"genero\":\"Drama\",\"director\":\"X\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.anio").isNotEmpty());
    }

    @Test
    void invalidUpdateDoesNotModifyMovie() throws Exception {
        long id = createAsAdmin(ALIEN);

        send(put("/api/peliculas/" + id), admin(), withYear(5)).andExpect(status().isBadRequest());

        mvc.perform(get("/api/peliculas/" + id))
                .andExpect(jsonPath("$.titulo").value("Alien"))
                .andExpect(jsonPath("$.anio").value(1979));
    }

    private static String withYear(int year) {
        return "{\"titulo\":\"Alien\",\"anio\":" + year + ",\"genero\":\"Drama\",\"director\":\"X\",\"precio\":12.99}";
    }

    @Test
    void listingIsPublicAndPaged() throws Exception {
        for (int i = 0; i < 3; i++) {
            createAsAdmin(ALIEN);
        }

        mvc.perform(get("/api/peliculas").param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].titulo").value("Alien"))
                .andExpect(jsonPath("$.content[0].valoracionMedia").isEmpty());

        mvc.perform(get("/api/peliculas").param("size", "2"))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").isNumber());
    }

    @Test
    void searchWithNoResultsReturnsEmptyListWith200() throws Exception {
        createAsAdmin(ALIEN);

        mvc.perform(get("/api/peliculas").param("titulo", "does-not-exist-xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0));

        mvc.perform(get("/api/peliculas").param("titulo", "alien").param("genero", "Western"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    private static String json(String title, int year, String genre, String director) {
        return "{\"titulo\":\"" + title + "\",\"anio\":" + year + ",\"genero\":\"" + genre
                + "\",\"director\":\"" + director + "\",\"precio\":12.99}";
    }

    @Test
    void searchesAndFiltersByTitleGenreYearAndDirector() throws Exception {
        createAsAdmin(json("Zeta One", 1991, "GeneroZ", "DirectorZ"));
        createAsAdmin(json("Zeta Two", 1992, "GeneroZ", "DirectorZ"));
        createAsAdmin(json("Other", 1992, "GeneroZ", "OtherDirector"));

        mvc.perform(get("/api/peliculas").param("titulo", "ZETA").param("genero", "GeneroZ"))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mvc.perform(get("/api/peliculas").param("genero", "GeneroZ").param("anio", "1992"))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mvc.perform(get("/api/peliculas").param("director", "DirectorZ").param("anio", "1992"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value("Zeta Two"));
        mvc.perform(get("/api/peliculas").param("titulo", "zeta").param("director", "OtherDirector"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void nonNumericYearReturns400() throws Exception {
        mvc.perform(get("/api/peliculas").param("anio", "abc")).andExpect(status().isBadRequest());
    }

    // ---------- price (Requirement 1 of carrito-compras-api-first) ----------

    @Test
    void createWithValidPriceStoresAndReturnsIt() throws Exception {
        String body = send(post("/api/peliculas"), admin(), ALIEN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precio").value(12.99))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        mvc.perform(get("/api/peliculas/" + id)).andExpect(jsonPath("$.precio").value(12.99));
        mvc.perform(get("/api/peliculas").param("titulo", "Alien"))
                .andExpect(jsonPath("$.content[0].precio").value(12.99));
    }

    @Test
    void createOrUpdateWithoutPriceReturns400() throws Exception {
        send(post("/api/peliculas"), admin(), "{\"titulo\":\"Alien\",\"anio\":1979,\"genero\":\"Drama\",\"director\":\"X\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precio").isNotEmpty());
    }

    @Test
    void createWithZeroOrNegativePriceReturns400() throws Exception {
        send(post("/api/peliculas"), admin(), withPrice("0")).andExpect(status().isBadRequest());
        send(post("/api/peliculas"), admin(), withPrice("-5.00")).andExpect(status().isBadRequest());
    }

    @Test
    void createWithMoreThanTwoDecimalsReturns400() throws Exception {
        send(post("/api/peliculas"), admin(), withPrice("12.999")).andExpect(status().isBadRequest());
    }

    @Test
    void createWithMoreThanEightIntegerDigitsReturns400() throws Exception {
        send(post("/api/peliculas"), admin(), withPrice("99999999.99")).andExpect(status().isCreated());
        send(post("/api/peliculas"), admin(), withPrice("100000000")).andExpect(status().isBadRequest());
    }

    @Test
    void updateChangesPrice() throws Exception {
        long id = createAsAdmin(ALIEN);

        send(put("/api/peliculas/" + id), admin(),
                "{\"titulo\":\"Alien\",\"anio\":1979,\"genero\":\"Science fiction\",\"director\":\"Ridley Scott\",\"precio\":19.99}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(19.99));

        mvc.perform(get("/api/peliculas/" + id)).andExpect(jsonPath("$.precio").value(19.99));
    }

    @Test
    void preexistingMovieWithoutPriceIsNotPurchasableInApi() throws Exception {
        // Simulates a movie created before this change: it is saved without a price through another path
        var preexisting = new shop.catalog.internal.domain.model.Movie();
        preexisting.setTitle("Silent");
        preexisting.setYear(1950);
        preexisting.setGenre("Drama");
        preexisting.setDirector("Somebody");
        var saved = repo.save(preexisting);

        mvc.perform(get("/api/peliculas/" + saved.getId()))
                .andExpect(jsonPath("$.precio").isEmpty());
    }

    private static String withPrice(String price) {
        return "{\"titulo\":\"Alien\",\"anio\":1979,\"genero\":\"Drama\",\"director\":\"X\",\"precio\":" + price + "}";
    }
}

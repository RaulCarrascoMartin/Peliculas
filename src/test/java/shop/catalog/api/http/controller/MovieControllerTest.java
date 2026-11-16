package shop.catalog.api.http.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import shop.catalog.api.http.mapper.MovieWebMapperImpl;
import shop.catalog.internal.domain.dto.MovieDetailsDto;
import shop.catalog.internal.domain.dto.MovieDto;
import shop.catalog.internal.domain.exception.InvalidSortPropertyException;
import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.service.MovieService;
import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;
import shop.shared.security.JwtTokens;
import shop.shared.security.SecurityConfig;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract of the catalog with the service mocked: codes, bodies, format validation, errors and security.
 *
 * <p>{@code @PreAuthorize} makes Spring proxy the controller. The application proxies the class (Boot's default,
 * {@link AopAutoConfiguration}); the MVC slice does not load that auto-configuration and would proxy the interface
 * {@code MovieHttpApi} instead, which is not a {@code @RestController}, so its routes would not be registered.
 */
@WebMvcTest(MovieController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import({SecurityConfig.class, JwtTokens.class, MovieWebMapperImpl.class})
class MovieControllerTest {

    private static final String ALIEN_JSON = """
            {"titulo":"Alien","anio":1979,"genero":"Science fiction","director":"Ridley Scott","precio":12.99,"stock":3}""";

    private static final MovieDetailsDto ALIEN_DETAILS =
            new MovieDetailsDto("Alien", 1979, "Science fiction", "Ridley Scott", new BigDecimal("12.99"), 3);

    private static final MovieDto ALIEN =
            new MovieDto(10L, "Alien", 1979, "Science fiction", "Ridley Scott", new BigDecimal("12.99"), 4.5, 3);

    private static final PageQuery DEFAULT_PAGE = new PageQuery(0, 20, List.of(new PageQuery.Sort("id", true)));

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @MockitoBean
    private MovieService movieService;

    private String admin() {
        return "Bearer " + tokens.issueToken(1L, "ADMIN");
    }

    private String user() {
        return "Bearer " + tokens.issueToken(2L, "USUARIO");
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String authorization, String json)
            throws Exception {
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(request);
    }

    private static String json(String title, String year, String genre, String director, String price, String stock) {
        return "{\"titulo\":" + title + ",\"anio\":" + year + ",\"genero\":" + genre + ",\"director\":" + director
                + ",\"precio\":" + price + ",\"stock\":" + stock + "}";
    }

    private static String withYear(String year) {
        return json("\"Alien\"", year, "\"Drama\"", "\"X\"", "12.99", "null");
    }

    private static String withPrice(String price) {
        return json("\"Alien\"", "1979", "\"Drama\"", "\"X\"", price, "null");
    }

    // ---------------------------------------------------------------- search

    @Test
    void searchIsPublicAndReturnsContentPlusPageWithTheSpanishNames() throws Exception {
        when(movieService.search(null, null, null, null, DEFAULT_PAGE))
                .thenReturn(new PageResult<>(List.of(ALIEN), 41, DEFAULT_PAGE));

        mvc.perform(get("/api/peliculas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].titulo").value("Alien"))
                .andExpect(jsonPath("$.content[0].anio").value(1979))
                .andExpect(jsonPath("$.content[0].genero").value("Science fiction"))
                .andExpect(jsonPath("$.content[0].director").value("Ridley Scott"))
                .andExpect(jsonPath("$.content[0].precio").value(12.99))
                .andExpect(jsonPath("$.content[0].valoracionMedia").value(4.5))
                .andExpect(jsonPath("$.content[0].stock").value(3))
                .andExpect(jsonPath("$.content[0].title").doesNotExist())
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(41))
                .andExpect(jsonPath("$.page.totalPages").value(3));
    }

    @Test
    void searchPassesFiltersPaginationAndSortToTheService() throws Exception {
        var query = new PageQuery(2, 5,
                List.of(new PageQuery.Sort("title", false), new PageQuery.Sort("year", true)));
        when(movieService.search("alien", "Drama", 1979, "Ridley Scott", query))
                .thenReturn(new PageResult<>(List.of(), 0, query));

        mvc.perform(get("/api/peliculas").param("titulo", "alien").param("genero", "Drama").param("anio", "1979")
                        .param("director", "Ridley Scott").param("page", "2").param("size", "5")
                        .param("sort", "title,desc").param("sort", "year"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.number").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$.page.totalPages").value(0));

        verify(movieService).search("alien", "Drama", 1979, "Ridley Scott", query);
    }

    @Test
    void paginationLimitsAreAccepted() throws Exception {
        when(movieService.search(any(), any(), any(), any(), any()))
                .thenAnswer(call -> new PageResult<MovieDto>(List.of(), 0, call.getArgument(4)));

        mvc.perform(get("/api/peliculas").param("page", "0").param("size", "1")).andExpect(status().isOk());
        mvc.perform(get("/api/peliculas").param("size", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(2000));
    }

    @Test
    void invalidPaginationReturns400WithTheParameterAndNeverReachesTheService() throws Exception {
        mvc.perform(get("/api/peliculas").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasItem(startsWith("page: "))));
        mvc.perform(get("/api/peliculas").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(startsWith("size: "))));
        mvc.perform(get("/api/peliculas").param("size", "2001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(startsWith("size: "))));

        verifyNoInteractions(movieService);
    }

    @Test
    void nonNumericParametersReturn400() throws Exception {
        mvc.perform(get("/api/peliculas").param("anio", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/peliculas").param("size", "abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/peliculas/abc")).andExpect(status().isBadRequest());

        verifyNoInteractions(movieService);
    }

    @Test
    void unknownSortPropertyIsTranslatedTo400AsProblemDetail() throws Exception {
        when(movieService.search(any(), any(), any(), any(), any())).thenThrow(new InvalidSortPropertyException("x"));

        mvc.perform(get("/api/peliculas").param("sort", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Propiedad de ordenación no válida: x"));
    }

    // ---------------------------------------------------------------- detail

    @Test
    void detailIsPublicAndReturns200WithTheMovieAnd404IfMissing() throws Exception {
        when(movieService.find(10L)).thenReturn(ALIEN);
        when(movieService.find(99L)).thenThrow(new MovieNotFoundException(99L));

        mvc.perform(get("/api/peliculas/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.titulo").value("Alien"))
                .andExpect(jsonPath("$.valoracionMedia").value(4.5));
        mvc.perform(get("/api/peliculas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Película 99 no encontrada"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void movieWithoutPriceNorRatingsShowsThemAsNull() throws Exception {
        when(movieService.find(11L))
                .thenReturn(new MovieDto(11L, "Silent", 1950, "Drama", "Somebody", null, null, 0));

        mvc.perform(get("/api/peliculas/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").isEmpty())
                .andExpect(jsonPath("$.valoracionMedia").isEmpty())
                .andExpect(jsonPath("$.stock").value(0));
    }

    // ---------------------------------------------------------------- create

    @Test
    void adminPostReturns201WithTheMovie() throws Exception {
        when(movieService.create(ALIEN_DETAILS)).thenReturn(ALIEN);

        send(post("/api/peliculas"), admin(), ALIEN_JSON)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.titulo").value("Alien"))
                .andExpect(jsonPath("$.anio").value(1979))
                .andExpect(jsonPath("$.genero").value("Science fiction"))
                .andExpect(jsonPath("$.director").value("Ridley Scott"))
                .andExpect(jsonPath("$.precio").value(12.99))
                .andExpect(jsonPath("$.stock").value(3));
    }

    @Test
    void stockIsOptionalInTheBody() throws Exception {
        when(movieService.create(any())).thenReturn(ALIEN);

        send(post("/api/peliculas"), admin(), withPrice("12.99")).andExpect(status().isCreated());

        verify(movieService).create(new MovieDetailsDto("Alien", 1979, "Drama", "X", new BigDecimal("12.99"), null));
    }

    @Test
    void badFormatReturns400WithTheFieldAndNeverReachesTheService() throws Exception {
        send(post("/api/peliculas"), admin(), "{\"titulo\":\"\",\"anio\":5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasItem(startsWith("titulo: "))))
                .andExpect(jsonPath("$.errors", hasItem(startsWith("anio: "))))
                .andExpect(jsonPath("$.errors", hasItem(startsWith("genero: "))))
                .andExpect(jsonPath("$.errors", hasItem(startsWith("director: "))))
                .andExpect(jsonPath("$.errors", hasItem(startsWith("precio: "))));
        send(post("/api/peliculas"), admin(), json("\"Alien\"", "1979", "\"Drama\"", "\"X\"", "12.99", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(startsWith("stock: "))));
        send(post("/api/peliculas"), admin(), "{no es json").andExpect(status().isBadRequest());

        verifyNoInteractions(movieService);
    }

    @Test
    void yearAndPriceBoundaries() throws Exception {
        when(movieService.create(any())).thenReturn(ALIEN);

        send(post("/api/peliculas"), admin(), withYear("1887")).andExpect(status().isBadRequest());
        send(post("/api/peliculas"), admin(), withYear("2101")).andExpect(status().isBadRequest());
        send(post("/api/peliculas"), admin(), withYear("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(startsWith("anio: "))));
        send(post("/api/peliculas"), admin(), withYear("1888")).andExpect(status().isCreated());
        send(post("/api/peliculas"), admin(), withYear("2100")).andExpect(status().isCreated());

        send(post("/api/peliculas"), admin(), withPrice("0")).andExpect(status().isBadRequest());
        send(post("/api/peliculas"), admin(), withPrice("-5.00")).andExpect(status().isBadRequest());
        send(post("/api/peliculas"), admin(), withPrice("12.999")).andExpect(status().isBadRequest());
        send(post("/api/peliculas"), admin(), withPrice("100000000")).andExpect(status().isBadRequest());
        send(post("/api/peliculas"), admin(), withPrice("0.01")).andExpect(status().isCreated());
        send(post("/api/peliculas"), admin(), withPrice("99999999.99")).andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------- update

    @Test
    void adminPutReturns200WithTheMovieAnd404IfMissing() throws Exception {
        when(movieService.update(10L, ALIEN_DETAILS)).thenReturn(ALIEN);
        when(movieService.update(99L, ALIEN_DETAILS)).thenThrow(new MovieNotFoundException(99L));

        send(put("/api/peliculas/10"), admin(), ALIEN_JSON)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.titulo").value("Alien"));
        send(put("/api/peliculas/99"), admin(), ALIEN_JSON)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Película 99 no encontrada"));
    }

    @Test
    void putWithBadFormatReturns400AndNeverReachesTheService() throws Exception {
        send(put("/api/peliculas/10"), admin(), "{\"titulo\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(startsWith("titulo: "))));

        verifyNoInteractions(movieService);
    }

    // ---------------------------------------------------------------- delete

    @Test
    void adminDeleteReturns204WithoutBodyAnd404IfMissing() throws Exception {
        doThrow(new MovieNotFoundException(99L)).when(movieService).delete(99L);

        send(delete("/api/peliculas/10"), admin(), null)
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        send(delete("/api/peliculas/99"), admin(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(movieService).delete(10L);
    }

    // ---------------------------------------------------------------- security

    @Test
    void withoutIdentityWritesReturn401AsProblemDetail() throws Exception {
        send(post("/api/peliculas"), null, ALIEN_JSON)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        send(put("/api/peliculas/10"), null, ALIEN_JSON).andExpect(status().isUnauthorized());
        send(delete("/api/peliculas/10"), null, null).andExpect(status().isUnauthorized());
        send(delete("/api/peliculas/10"), "Bearer no-es-un-token", null).andExpect(status().isUnauthorized());

        verifyNoInteractions(movieService);
    }

    @Test
    void nonAdminWritesReturn403AsProblemDetail() throws Exception {
        send(post("/api/peliculas"), user(), ALIEN_JSON)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        send(put("/api/peliculas/10"), user(), ALIEN_JSON).andExpect(status().isForbidden());
        send(delete("/api/peliculas/10"), user(), null).andExpect(status().isForbidden());

        verifyNoInteractions(movieService);
    }

    @Test
    void readsAreAllowedToAnyIdentity() throws Exception {
        when(movieService.find(10L)).thenReturn(ALIEN);

        send(get("/api/peliculas/10"), user(), null).andExpect(status().isOk());
        send(get("/api/peliculas/10"), admin(), null).andExpect(status().isOk());
    }
}

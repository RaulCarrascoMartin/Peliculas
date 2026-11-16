package shop.rating.api.http.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import shop.rating.api.http.mapper.MovieMarkWebMapperImpl;
import shop.rating.internal.domain.dto.CatalogMovieDto;
import shop.rating.internal.domain.dto.MovieMarkDto;
import shop.rating.internal.domain.exception.MovieAlreadyMarkedException;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.service.MovieMarkService;
import shop.shared.security.CurrentUser;
import shop.shared.security.JwtTokens;
import shop.shared.security.SecurityConfig;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP contract of the favourite / watched lists with the service mocked: codes, bodies, errors and security. */
@WebMvcTest(MovieMarkController.class)
@Import({SecurityConfig.class, JwtTokens.class, MovieMarkWebMapperImpl.class, CurrentUser.class})
class MovieMarkControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @MockitoBean
    private MovieMarkService movieMarkService;

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + tokens.issueToken(7L, "USUARIO"));
    }

    private static MovieMarkDto mark(Long movieId, MarkType type) {
        return new MovieMarkDto(null, 7L, movieId, type);
    }

    @Test
    void putMarksTheMovieInTheListOfTheRouteForTheAuthenticatedUserAndReturns204() throws Exception {
        mvc.perform(asUser(put("/api/peliculas/10/favorita")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(asUser(put("/api/peliculas/11/vista"))).andExpect(status().isNoContent());

        verify(movieMarkService).mark(mark(10L, MarkType.FAVORITA));
        verify(movieMarkService).mark(mark(11L, MarkType.VISTA));
        verifyNoMoreInteractions(movieMarkService);
    }

    @Test
    void deleteUnmarksTheMovieFromTheListOfTheRouteAndReturns204() throws Exception {
        mvc.perform(asUser(delete("/api/peliculas/10/favorita")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(asUser(delete("/api/peliculas/11/vista"))).andExpect(status().isNoContent());

        verify(movieMarkService).unmark(mark(10L, MarkType.FAVORITA));
        verify(movieMarkService).unmark(mark(11L, MarkType.VISTA));
        verifyNoMoreInteractions(movieMarkService);
    }

    @Test
    void listsReturn200WithTheMoviesOfEachListUnderTheContractNames() throws Exception {
        when(movieMarkService.listMarked(7L, MarkType.FAVORITA)).thenReturn(List.of(
                new CatalogMovieDto(10L, "Alien", 1979, "Horror", "Ridley Scott", 4.5),
                new CatalogMovieDto(11L, "Matrix", 1999, "Sci-fi", "Wachowski", null)));
        when(movieMarkService.listMarked(7L, MarkType.VISTA)).thenReturn(List.of());

        mvc.perform(asUser(get("/api/usuarios/me/favoritas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].titulo").value("Alien"))
                .andExpect(jsonPath("$[0].anio").value(1979))
                .andExpect(jsonPath("$[0].genero").value("Horror"))
                .andExpect(jsonPath("$[0].director").value("Ridley Scott"))
                .andExpect(jsonPath("$[0].valoracionMedia").value(4.5))
                .andExpect(jsonPath("$[0].title").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].valoracionMedia").isEmpty());
        mvc.perform(asUser(get("/api/usuarios/me/vistas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void domainFailuresOfMarkAreTranslatedTo404And409AsProblemDetail() throws Exception {
        doThrow(new MovieNotFoundException(99L)).when(movieMarkService).mark(mark(99L, MarkType.FAVORITA));
        doThrow(new MovieNotFoundException(99L)).when(movieMarkService).mark(mark(99L, MarkType.VISTA));
        doThrow(new MovieAlreadyMarkedException(10L, MarkType.VISTA))
                .when(movieMarkService).mark(mark(10L, MarkType.VISTA));

        mvc.perform(asUser(put("/api/peliculas/99/favorita")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Película 99 no encontrada"));
        mvc.perform(asUser(put("/api/peliculas/99/vista"))).andExpect(status().isNotFound());
        mvc.perform(asUser(put("/api/peliculas/10/vista")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("La película 10 ya está marcada como VISTA"));
    }

    @Test
    void nonNumericMovieIdReturns400AndNeverReachesTheService() throws Exception {
        mvc.perform(asUser(put("/api/peliculas/abc/favorita")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mvc.perform(asUser(delete("/api/peliculas/abc/vista"))).andExpect(status().isBadRequest());

        verifyNoInteractions(movieMarkService);
    }

    @Test
    void withoutIdentityEveryRouteReturns401AsProblemDetail() throws Exception {
        mvc.perform(put("/api/peliculas/1/favorita"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(delete("/api/peliculas/1/favorita")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/peliculas/1/vista")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/peliculas/1/vista")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me/favoritas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me/vistas").header("Authorization", "Bearer no-es-un-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(movieMarkService);
    }
}

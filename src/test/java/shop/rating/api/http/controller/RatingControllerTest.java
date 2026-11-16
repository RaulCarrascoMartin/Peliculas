package shop.rating.api.http.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import shop.rating.api.http.mapper.RatingWebMapperImpl;
import shop.rating.internal.domain.dto.RateMovieDto;
import shop.rating.internal.domain.dto.RatingDto;
import shop.rating.internal.domain.exception.InvalidScoreException;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.exception.RatingAlreadyExistsException;
import shop.rating.internal.domain.service.RatingService;
import shop.shared.security.CurrentUser;
import shop.shared.security.JwtTokens;
import shop.shared.security.SecurityConfig;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP contract of ratings with the service mocked: codes, bodies, format validation, errors and security. */
@WebMvcTest(RatingController.class)
@Import({SecurityConfig.class, JwtTokens.class, RatingWebMapperImpl.class, CurrentUser.class})
class RatingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @MockitoBean
    private RatingService ratingService;

    private String bearer() {
        return "Bearer " + tokens.issueToken(7L, "USUARIO");
    }

    private ResultActions rate(String movieId, String json) throws Exception {
        return mvc.perform(put("/api/peliculas/" + movieId + "/valoracion").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void validPutReturns200WithTheRatingOfTheAuthenticatedUser() throws Exception {
        when(ratingService.rate(new RateMovieDto(7L, 10L, 4, "Very good")))
                .thenReturn(new RatingDto(5L, 7L, 10L, 4, "Very good"));

        rate("10", "{\"puntuacion\":4,\"comentario\":\"Very good\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.peliculaId").value(10))
                .andExpect(jsonPath("$.puntuacion").value(4))
                .andExpect(jsonPath("$.comentario").value("Very good"))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.movieId").doesNotExist());
    }

    @Test
    void commentIsOptionalAndBoundariesOfTheScoreAreAccepted() throws Exception {
        when(ratingService.rate(new RateMovieDto(7L, 10L, 1, null))).thenReturn(new RatingDto(5L, 7L, 10L, 1, null));
        when(ratingService.rate(new RateMovieDto(7L, 10L, 5, null))).thenReturn(new RatingDto(5L, 7L, 10L, 5, null));

        rate("10", "{\"puntuacion\":1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puntuacion").value(1))
                .andExpect(jsonPath("$.comentario").isEmpty());
        rate("10", "{\"puntuacion\":5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puntuacion").value(5));
    }

    @Test
    void badFormatReturns400WithTheFieldAndNeverReachesTheService() throws Exception {
        rate("10", "{\"puntuacion\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasItem(startsWith("puntuacion: "))));
        rate("10", "{\"puntuacion\":6}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(startsWith("puntuacion: "))));
        rate("10", "{\"comentario\":\"no note\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(startsWith("puntuacion: "))));
        rate("10", "not json").andExpect(status().isBadRequest());
        rate("abc", "{\"puntuacion\":3}").andExpect(status().isBadRequest());

        verifyNoInteractions(ratingService);
    }

    @Test
    void domainFailuresAreTranslatedTo404And400And409AsProblemDetail() throws Exception {
        when(ratingService.rate(any())).thenThrow(new MovieNotFoundException(99L));
        rate("99", "{\"puntuacion\":3}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Película 99 no encontrada"))
                .andExpect(jsonPath("$.errors").doesNotExist());

        reset(ratingService);
        when(ratingService.rate(any())).thenThrow(new InvalidScoreException(1, 5));
        rate("10", "{\"puntuacion\":3}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("La puntuación debe estar entre 1 y 5"));

        reset(ratingService);
        when(ratingService.rate(any())).thenThrow(new RatingAlreadyExistsException(10L));
        rate("10", "{\"puntuacion\":3}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Ya existe una valoración tuya de la película 10"));
    }

    @Test
    void withoutIdentityReturns401AsProblemDetail() throws Exception {
        mvc.perform(put("/api/peliculas/10/valoracion")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"puntuacion\":3}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(put("/api/peliculas/10/valoracion").header("Authorization", "Bearer no-es-un-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"puntuacion\":3}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ratingService);
    }
}

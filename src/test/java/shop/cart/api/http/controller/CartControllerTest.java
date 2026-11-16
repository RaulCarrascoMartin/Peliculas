package shop.cart.api.http.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import shop.cart.api.http.mapper.CartWebMapperImpl;
import shop.cart.internal.domain.dto.CartContentsDto;
import shop.cart.internal.domain.dto.PricedLineDto;
import shop.cart.internal.domain.exception.MovieAlreadyInCartException;
import shop.cart.internal.domain.exception.MovieNotFoundException;
import shop.cart.internal.domain.exception.MovieNotInCartException;
import shop.cart.internal.domain.exception.MovieWithoutPriceException;
import shop.cart.internal.domain.service.CartService;
import shop.shared.security.CurrentUser;
import shop.shared.security.JwtTokens;
import shop.shared.security.SecurityConfig;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP contract of the cart with the service mocked: codes, bodies, format validation, errors and security. */
@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, JwtTokens.class, CartWebMapperImpl.class, CurrentUser.class})
class CartControllerTest {

    private static final long USER = 7L;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @MockitoBean
    private CartService cartService;

    private String bearer() {
        return "Bearer " + tokens.issueToken(USER, "USUARIO");
    }

    private ResultActions add(String json) throws Exception {
        return mvc.perform(post("/api/carrito/lineas").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void getReturns200WithTheCartOfTheTokenUser() throws Exception {
        when(cartService.contentsOf(USER)).thenReturn(new CartContentsDto(List.of(
                new PricedLineDto(10L, "Alien", new BigDecimal("12.99")),
                new PricedLineDto(11L, "Matrix", new BigDecimal("7.50"))), new BigDecimal("20.49")));

        mvc.perform(get("/api/carrito").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas.length()").value(2))
                .andExpect(jsonPath("$.lineas[0].peliculaId").value(10))
                .andExpect(jsonPath("$.lineas[0].titulo").value("Alien"))
                .andExpect(jsonPath("$.lineas[0].precio").value(12.99))
                .andExpect(jsonPath("$.lineas[1].peliculaId").value(11))
                .andExpect(jsonPath("$.total").value(20.49));
    }

    @Test
    void getOfEmptyCartReturnsNoLinesAndZeroTotal() throws Exception {
        when(cartService.contentsOf(USER)).thenReturn(new CartContentsDto(List.of(), BigDecimal.ZERO));

        mvc.perform(get("/api/carrito").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void validPostReturns201WithTheLine() throws Exception {
        when(cartService.addMovie(USER, 10L)).thenReturn(new PricedLineDto(10L, "Alien", new BigDecimal("12.99")));

        add("{\"peliculaId\":10}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.peliculaId").value(10))
                .andExpect(jsonPath("$.titulo").value("Alien"))
                .andExpect(jsonPath("$.precio").value(12.99));
    }

    @Test
    void userIdInTheBodyIsIgnored() throws Exception {
        when(cartService.addMovie(USER, 10L)).thenReturn(new PricedLineDto(10L, "Alien", new BigDecimal("12.99")));

        add("{\"peliculaId\":10,\"usuarioId\":99}").andExpect(status().isCreated());

        verify(cartService).addMovie(USER, 10L);
    }

    @Test
    void badFormatReturns400WithTheFieldAndNeverReachesTheService() throws Exception {
        add("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasItem(startsWith("peliculaId: "))));
        add("not json").andExpect(status().isBadRequest());
        add("{\"peliculaId\":\"abc\"}").andExpect(status().isBadRequest());
        mvc.perform(delete("/api/carrito/lineas/abc").header("Authorization", bearer()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    void domainFailuresOfAddAreTranslatedTo404And409AsProblemDetail() throws Exception {
        when(cartService.addMovie(USER, 99L)).thenThrow(new MovieNotFoundException(99L));
        when(cartService.addMovie(USER, 10L)).thenThrow(new MovieAlreadyInCartException(10L));
        when(cartService.addMovie(USER, 11L)).thenThrow(new MovieWithoutPriceException(11L));

        add("{\"peliculaId\":99}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("La película 99 no existe"))
                .andExpect(jsonPath("$.errors").doesNotExist());
        add("{\"peliculaId\":10}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("La película 10 ya está en el carrito"));
        add("{\"peliculaId\":11}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("La película 11 no tiene precio y no es comprable"));
    }

    @Test
    void removeLineReturns204And404IfTheMovieIsNotInTheCart() throws Exception {
        doThrow(new MovieNotInCartException(99L)).when(cartService).removeMovie(USER, 99L);

        mvc.perform(delete("/api/carrito/lineas/10").header("Authorization", bearer()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(cartService).removeMovie(USER, 10L);

        mvc.perform(delete("/api/carrito/lineas/99").header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("La película 99 no está en el carrito"));
    }

    @Test
    void clearReturns204() throws Exception {
        mvc.perform(delete("/api/carrito").header("Authorization", bearer()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(cartService).clear(USER);
    }

    @Test
    void lostOptimisticLockIsA409ThatExposesNoInternals() throws Exception {
        doThrow(new OptimisticLockingFailureException("Cart 50 was updated or deleted by another transaction"))
                .when(cartService).clear(USER);

        mvc.perform(delete("/api/carrito").header("Authorization", bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("El recurso fue modificado por otra petición; inténtalo de nuevo"));
    }

    @Test
    void withoutIdentityEveryRouteReturns401AsProblemDetail() throws Exception {
        mvc.perform(get("/api/carrito"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/api/carrito/lineas").contentType(MediaType.APPLICATION_JSON).content("{\"peliculaId\":1}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/carrito/lineas/1")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/carrito").header("Authorization", "Bearer no-es-un-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cartService);
    }
}

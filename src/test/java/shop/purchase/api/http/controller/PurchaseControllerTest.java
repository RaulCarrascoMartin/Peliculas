package shop.purchase.api.http.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.purchase.api.http.mapper.PurchaseWebMapperImpl;
import shop.purchase.internal.domain.dto.PurchaseDto;
import shop.purchase.internal.domain.dto.PurchaseLineDto;
import shop.purchase.internal.domain.exception.EmptyCartException;
import shop.purchase.internal.domain.exception.MovieNoLongerAvailableException;
import shop.purchase.internal.domain.exception.MovieWithoutPriceException;
import shop.purchase.internal.domain.exception.PurchaseNotFoundException;
import shop.purchase.internal.domain.model.PurchaseStatus;
import shop.purchase.internal.domain.service.PurchaseService;
import shop.shared.security.CurrentUser;
import shop.shared.security.JwtTokens;
import shop.shared.security.SecurityConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP contract of purchases with the service mocked: codes, bodies, errors and security. */
@WebMvcTest(PurchaseController.class)
@Import({SecurityConfig.class, JwtTokens.class, PurchaseWebMapperImpl.class, CurrentUser.class})
class PurchaseControllerTest {

    private static final LocalDateTime PURCHASED_AT = LocalDateTime.of(2026, 11, 2, 10, 15, 30);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @MockitoBean
    private PurchaseService purchaseService;

    private static PurchaseDto purchase(Long id) {
        return new PurchaseDto(id, 7L, PURCHASED_AT, new BigDecimal("17.48"), PurchaseStatus.COMPRADA, List.of(
                new PurchaseLineDto(100L, id, 1L, "Matrix", new BigDecimal("7.99")),
                new PurchaseLineDto(101L, id, 2L, "Inception", new BigDecimal("9.49"))));
    }

    private String bearer() {
        return "Bearer " + tokens.issueToken(7L, "USUARIO");
    }

    @Test
    void postReturns201WithThePurchaseOfTheUserOfTheToken() throws Exception {
        when(purchaseService.checkout(7L)).thenReturn(purchase(10L));

        String body = mvc.perform(post("/api/compras").header("Authorization", bearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.estado").value("COMPRADA"))
                .andExpect(jsonPath("$.total").value(17.48))
                .andExpect(jsonPath("$.lineas", hasSize(2)))
                .andExpect(jsonPath("$.lineas[0].peliculaId").value(1))
                .andExpect(jsonPath("$.lineas[0].titulo").value("Matrix"))
                .andExpect(jsonPath("$.lineas[0].precioUnitario").value(7.99))
                .andExpect(jsonPath("$.lineas[1].titulo").value("Inception"))
                .andExpect(jsonPath("$.usuarioId").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertEquals(PURCHASED_AT.atZone(ZoneId.systemDefault()).toInstant(),
                OffsetDateTime.parse(JsonPath.<String>read(body, "$.fecha")).toInstant());
    }

    @Test
    void userInTheRequestIsIgnoredAndTheOneOfTheTokenIsUsed() throws Exception {
        when(purchaseService.checkout(7L)).thenReturn(purchase(10L));
        when(purchaseService.historyOf(7L)).thenReturn(List.of());

        mvc.perform(post("/api/compras?usuarioId=99").header("Authorization", bearer()))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/compras?usuarioId=99").header("Authorization", bearer()))
                .andExpect(status().isOk());

        verify(purchaseService).checkout(7L);
        verify(purchaseService).historyOf(7L);
    }

    @Test
    void domainFailuresOfCheckoutAreTranslatedTo409AsProblemDetail() throws Exception {
        when(purchaseService.checkout(7L)).thenThrow(new EmptyCartException());
        mvc.perform(post("/api/compras").header("Authorization", bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("El carrito está vacío"))
                .andExpect(jsonPath("$.errors").doesNotExist());

        reset(purchaseService);
        when(purchaseService.checkout(7L)).thenThrow(new MovieNoLongerAvailableException(11L));
        mvc.perform(post("/api/compras").header("Authorization", bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La película 11 ya no está en el catálogo"));

        reset(purchaseService);
        when(purchaseService.checkout(7L)).thenThrow(new MovieWithoutPriceException(10L));
        mvc.perform(post("/api/compras").header("Authorization", bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La película 10 no tiene precio y no es comprable"));
    }

    @Test
    void lostOptimisticLockOfADoubleCheckoutIs409WithoutTechnicalDetails() throws Exception {
        when(purchaseService.checkout(7L))
                .thenThrow(new OptimisticLockingFailureException("update carritos set version = ... where id = 3"));

        mvc.perform(post("/api/compras").header("Authorization", bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("El recurso fue modificado por otra petición; inténtalo de nuevo"));
    }

    @Test
    void listReturns200WithThePurchasesInTheOrderOfTheServiceOrAnEmptyArray() throws Exception {
        when(purchaseService.historyOf(7L)).thenReturn(List.of(purchase(11L), purchase(10L)));

        mvc.perform(get("/api/compras").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[1].id").value(10))
                .andExpect(jsonPath("$[0].lineas[0].titulo").value("Matrix"));

        reset(purchaseService);
        when(purchaseService.historyOf(7L)).thenReturn(List.of());
        mvc.perform(get("/api/compras").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void detailReturns200WithThePurchaseAnd404IfMissingOrSomeoneElses() throws Exception {
        when(purchaseService.detailOf(7L, 10L)).thenReturn(purchase(10L));
        when(purchaseService.detailOf(7L, 99L)).thenThrow(new PurchaseNotFoundException(99L));

        mvc.perform(get("/api/compras/10").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.lineas[1].precioUnitario").value(9.49));
        mvc.perform(get("/api/compras/99").header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("La compra 99 no existe"));
    }

    @Test
    void nonNumericPurchaseIdReturns400AndNeverReachesTheService() throws Exception {
        mvc.perform(get("/api/compras/abc").header("Authorization", bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(purchaseService);
    }

    @Test
    void withoutIdentityEveryRouteReturns401AsProblemDetail() throws Exception {
        mvc.perform(post("/api/compras"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/compras")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/compras/10").header("Authorization", "Bearer no-es-un-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(purchaseService);
    }
}

package shop.orders.api.http.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import shop.orders.api.http.mapper.OrderWebMapperImpl;
import shop.orders.internal.domain.dto.CreateOrderDto;
import shop.orders.internal.domain.dto.OrderDto;
import shop.orders.internal.domain.dto.OrderLineDto;
import shop.orders.internal.domain.dto.StockItemDto;
import shop.orders.internal.domain.exception.InsufficientStockException;
import shop.orders.internal.domain.exception.OrderCannotBeCancelledException;
import shop.orders.internal.domain.exception.OrderNotFoundException;
import shop.orders.internal.domain.exception.ProductNotFoundException;
import shop.orders.internal.domain.model.OrderStatus;
import shop.orders.internal.domain.service.OrderService;
import shop.shared.security.JwtTokens;
import shop.shared.security.CurrentUser;
import shop.shared.security.SecurityConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP contract of orders with the service mocked: codes, bodies, format validation, errors and security. */
@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtTokens.class, OrderWebMapperImpl.class, CurrentUser.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @MockitoBean
    private OrderService orderService;

    private static OrderDto order(OrderStatus status) {
        return new OrderDto(10L, 7L, status, Instant.parse("2026-11-02T10:15:30Z"), new BigDecimal("25.98"),
                List.of(new OrderLineDto(1L, "Alien", new BigDecimal("12.99"), 2)));
    }

    private String bearer() {
        return "Bearer " + tokens.issueToken(7L, "USUARIO");
    }

    private ResultActions create(String json) throws Exception {
        return mvc.perform(post("/api/orders").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String body(long productId, int quantity) {
        return "{\"lines\":[{\"productId\":" + productId + ",\"quantity\":" + quantity + "}]}";
    }

    @Test
    void validPostReturns201WithTheOrder() throws Exception {
        when(orderService.create(new CreateOrderDto(7L, List.of(new StockItemDto(1L, 2)))))
                .thenReturn(order(OrderStatus.CREATED));

        create(body(1, 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.createdAt").value("2026-11-02T10:15:30Z"))
                .andExpect(jsonPath("$.total").value(25.98))
                .andExpect(jsonPath("$.lines[0].productId").value(1))
                .andExpect(jsonPath("$.lines[0].title").value("Alien"))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(12.99))
                .andExpect(jsonPath("$.lines[0].quantity").value(2));
    }

    @Test
    void badFormatReturns400WithTheFieldAndNeverReachesTheService() throws Exception {
        create("{\"lines\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(startsWith("lines: "))));
        create("{\"lines\":[{\"productId\":1,\"quantity\":0}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(startsWith("lines[0].quantity: "))));
        create("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(startsWith("lines: "))));

        verifyNoInteractions(orderService);
    }

    @Test
    void domainFailuresOfCreateAreTranslatedTo404And409AsProblemDetail() throws Exception {
        when(orderService.create(any())).thenThrow(new ProductNotFoundException(99L));
        create(body(99, 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("El producto 99 no existe"))
                .andExpect(jsonPath("$.errors").doesNotExist());

        reset(orderService);
        when(orderService.create(any())).thenThrow(new InsufficientStockException(1L));
        create(body(1, 6))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getReturns200WithTheOrderAnd404IfMissing() throws Exception {
        when(orderService.find(7L, 10L)).thenReturn(order(OrderStatus.CREATED));
        when(orderService.find(7L, 99L)).thenThrow(new OrderNotFoundException(99L));

        mvc.perform(get("/api/orders/10").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"));
        mvc.perform(get("/api/orders/99").header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("El pedido 99 no existe"));
    }

    @Test
    void cancelReturns200And409IfNotCancellableAnd404IfMissing() throws Exception {
        when(orderService.cancel(7L, 10L)).thenReturn(order(OrderStatus.CANCELLED));
        when(orderService.cancel(7L, 11L)).thenThrow(new OrderCannotBeCancelledException(11L));
        when(orderService.cancel(7L, 99L)).thenThrow(new OrderNotFoundException(99L));

        mvc.perform(post("/api/orders/10/cancel").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/orders/11/cancel").header("Authorization", bearer()))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/orders/99/cancel").header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void withoutIdentityEveryRouteReturns401AsProblemDetail() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body(1, 1)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/orders/10")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/orders/10/cancel").header("Authorization", "Bearer no-es-un-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }
}

package shop.orders.internal.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import shop.shared.error.GlobalExceptionHandler;
import shop.orders.internal.usecase.CancelOrder;
import shop.orders.internal.usecase.CatalogApiFake;
import shop.orders.internal.usecase.CreateOrder;
import shop.orders.internal.usecase.GetOrder;
import shop.orders.internal.usecase.OrderRepositoryFake;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Orders presentation layer with the modules doubled: catalog and persistence in memory. */
class OrdersControllerTest {

    private CatalogApiFake catalog;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        catalog = new CatalogApiFake();
        catalog.createProduct(1L, "Alien", "12.99", 5);
        var orders = new OrderRepositoryFake();
        mvc = MockMvcBuilders.standaloneSetup(new OrdersController(
                        new CreateOrder(catalog, orders), new GetOrder(orders), new CancelOrder(catalog, orders)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ResultActions create(String json) throws Exception {
        return mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String quantities(long productId, int quantity) {
        return "{\"userId\":7,\"lines\":[{\"productId\":" + productId + ",\"quantity\":" + quantity + "}]}";
    }

    @Test
    void validPostReturns201WithOrderAndCapturedPrices() throws Exception {
        create(quantities(1, 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.total").value(25.98))
                .andExpect(jsonPath("$.lines[0].productId").value(1))
                .andExpect(jsonPath("$.lines[0].title").value("Alien"))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(12.99))
                .andExpect(jsonPath("$.lines[0].quantity").value(2));
    }

    @Test
    void invalidPostReturns400() throws Exception {
        create("{\"userId\":7,\"lines\":[]}").andExpect(status().isBadRequest());
        create("{\"userId\":7,\"lines\":[{\"productId\":1,\"quantity\":0}]}").andExpect(status().isBadRequest());
        create("{\"lines\":[{\"productId\":1,\"quantity\":1}]}").andExpect(status().isBadRequest());
    }

    @Test
    void postWithUnknownProductReturns404() throws Exception {
        create(quantities(99, 1)).andExpect(status().isNotFound());
    }

    @Test
    void postWithoutStockReturns409() throws Exception {
        create(quantities(1, 6)).andExpect(status().isConflict());
    }

    @Test
    void getReturns200WithOrderAnd404IfMissing() throws Exception {
        create(quantities(1, 1)).andExpect(status().isCreated());

        mvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"));
        mvc.perform(get("/api/orders/99")).andExpect(status().isNotFound());
    }

    @Test
    void cancelReturns200Then409AndThen404IfMissing() throws Exception {
        create(quantities(1, 1)).andExpect(status().isCreated());

        mvc.perform(post("/api/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/orders/1/cancel")).andExpect(status().isConflict());
        mvc.perform(post("/api/orders/99/cancel")).andExpect(status().isNotFound());
    }
}

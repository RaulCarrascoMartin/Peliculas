package shop.orders.internal.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import shop.compartido.error.GlobalExceptionHandler;
import shop.orders.internal.usecase.CancelOrder;
import shop.orders.internal.usecase.CatalogApiFalsa;
import shop.orders.internal.usecase.CreateOrder;
import shop.orders.internal.usecase.GetOrder;
import shop.orders.internal.usecase.OrderRepositoryFalso;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Capa de presentación de pedidos con los módulos doblados: catálogo y persistencia en memoria. */
class OrdersControllerTest {

    private CatalogApiFalsa catalogo;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        catalogo = new CatalogApiFalsa();
        catalogo.crearProducto(1L, "Alien", "12.99", 5);
        var pedidos = new OrderRepositoryFalso();
        mvc = MockMvcBuilders.standaloneSetup(new OrdersController(
                        new CreateOrder(catalogo, pedidos), new GetOrder(pedidos), new CancelOrder(catalogo, pedidos)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ResultActions crear(String json) throws Exception {
        return mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String pedido(long productId, int quantity) {
        return "{\"userId\":7,\"lines\":[{\"productId\":" + productId + ",\"quantity\":" + quantity + "}]}";
    }

    @Test
    void postValidoDevuelve201ConElPedidoYLosPreciosCapturados() throws Exception {
        crear(pedido(1, 2))
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
    void postInvalidoDevuelve400() throws Exception {
        crear("{\"userId\":7,\"lines\":[]}").andExpect(status().isBadRequest());
        crear("{\"userId\":7,\"lines\":[{\"productId\":1,\"quantity\":0}]}").andExpect(status().isBadRequest());
        crear("{\"lines\":[{\"productId\":1,\"quantity\":1}]}").andExpect(status().isBadRequest());
    }

    @Test
    void postConProductoDesconocidoDevuelve404() throws Exception {
        crear(pedido(99, 1)).andExpect(status().isNotFound());
    }

    @Test
    void postSinStockDevuelve409() throws Exception {
        crear(pedido(1, 6)).andExpect(status().isConflict());
    }

    @Test
    void getDevuelve200ConElPedidoYDa404SiNoExiste() throws Exception {
        crear(pedido(1, 1)).andExpect(status().isCreated());

        mvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"));
        mvc.perform(get("/api/orders/99")).andExpect(status().isNotFound());
    }

    @Test
    void cancelDevuelve200Despues409YDa404SiNoExiste() throws Exception {
        crear(pedido(1, 1)).andExpect(status().isCreated());

        mvc.perform(post("/api/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/orders/1/cancel")).andExpect(status().isConflict());
        mvc.perform(post("/api/orders/99/cancel")).andExpect(status().isNotFound());
    }
}

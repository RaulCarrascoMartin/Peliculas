package shop.purchase.api.http.controller;

import com.jayway.jsonpath.JsonPath;
import shop.shared.security.JwtTokens;
import shop.purchase.internal.persistence.entity.PurchaseEntity;
import shop.purchase.internal.persistence.jpa.PurchaseJpaRepository;
import shop.purchase.internal.persistence.jpa.PurchaseLineJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Double simultaneous checkout of the same cart through the real HTTP layer (no doubles): verifies the
 * real mechanism —{@code @Version} of {@code Cart} + transaction + 409 handler—, not the
 * exception conversion. It is repeated for several rounds with a barrier to force the overlap; whatever
 * the interleaving, the correct result is the same: one purchase and one 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConcurrentCheckoutIntegrationTest {

    private static final int ROUNDS = 25;
    private static final long FIRST_USER = 4001;
    private static final String ALIEN = """
            {"titulo":"Alien","anio":1979,"genero":"Science fiction","director":"Ridley Scott","precio":12.99}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @Autowired
    private PurchaseJpaRepository purchases;

    @Autowired
    private PurchaseLineJpaRepository lines;

    private final List<Long> usedUsers = new ArrayList<>();
    private final List<Long> createdMovies = new ArrayList<>();

    @AfterEach
    void clean() throws Exception {
        for (long userId : usedUsers) {
            for (PurchaseEntity purchase : purchases.findByUserIdOrderByPurchasedAtDesc(userId)) {
                lines.deleteAll(lines.findByPurchaseIdOrderById(purchase.getId()));
                purchases.delete(purchase);
            }
            mvc.perform(delete("/api/carrito").header("Authorization", bearer(userId)));
        }
        for (long id : createdMovies) {
            mvc.perform(delete("/api/peliculas/" + id).header("Authorization", bearerAdmin()));
        }
    }

    private String bearer(long userId) {
        if (!usedUsers.contains(userId)) {
            usedUsers.add(userId);
        }
        return "Bearer " + tokens.issueToken(userId, "USUARIO");
    }

    private String bearerAdmin() {
        return "Bearer " + tokens.issueToken(1L, "ADMIN");
    }

    private long createMovie() throws Exception {
        String body = mvc.perform(post("/api/peliculas").header("Authorization", bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(ALIEN))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        createdMovies.add(id);
        return id;
    }

    private int complete(long userId) throws Exception {
        return mvc.perform(post("/api/compras").header("Authorization", bearer(userId)))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void twoSimultaneousCheckoutsCreateOnePurchaseAndTheOtherGets409() throws Exception {
        long movie = createMovie();
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                long user = FIRST_USER + round;
                mvc.perform(post("/api/carrito/lineas").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"peliculaId\":" + movie + "}"));

                var barrier = new CyclicBarrier(2);
                Callable<Integer> checkout = () -> {
                    barrier.await();
                    return complete(user);
                };
                Future<Integer> a = threads.submit(checkout);
                Future<Integer> b = threads.submit(checkout);
                List<Integer> statuses = List.of(a.get(), b.get()).stream().sorted().toList();

                assertEquals(List.of(201, 409), statuses, "round " + round + ": one purchase and one conflict");
                assertEquals(1, purchases.findByUserIdOrderByPurchasedAtDesc(user).size(),
                        "round " + round + ": exactly one purchase");
                String cart = mvc.perform(get("/api/carrito").header("Authorization", bearer(user)))
                        .andReturn().getResponse().getContentAsString();
                assertTrue(((List<?>) JsonPath.read(cart, "$.lineas")).isEmpty(),
                        "round " + round + ": empty cart at the end");
            }
        } finally {
            threads.shutdownNow();
        }
    }
}

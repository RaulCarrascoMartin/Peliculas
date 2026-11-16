package shop.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import shop.e2e.ApiClient.Reply;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests: the whole application on a real port, a real HTTP client and the real security chain, going
 * through every module (authentication, catalog, rating, cart, purchase and orders).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "peliculas.admin.email=e2e.admin@correo.com",
        "peliculas.admin.contrasena=ClaveAdmin123"
})
class ShopE2ETest {

    private static final String ADMIN_EMAIL = "e2e.admin@correo.com";
    private static final String ADMIN_PASSWORD = "ClaveAdmin123";
    private static final String PASSWORD = "Secreta123";

    @LocalServerPort
    private int port;

    private ApiClient api;
    private String adminToken;

    private ApiClient api() {
        if (api == null) {
            api = new ApiClient(port);
            adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        }
        return api;
    }

    private String login(String email, String password) {
        Reply reply = api.post("/api/auth/login",
                "{\"email\":\"%s\",\"contrasena\":\"%s\"}".formatted(email, password), null);
        assertEquals(200, reply.status(), reply.body());
        return reply.json("$.token");
    }

    /** Registers a brand-new user (unique email) and returns their token. */
    private String newUserToken() {
        String email = "e2e-" + UUID.randomUUID() + "@correo.com";
        Reply registered = api().post("/api/auth/registro",
                "{\"email\":\"%s\",\"contrasena\":\"%s\"}".formatted(email, PASSWORD), null);
        assertEquals(201, registered.status(), registered.body());
        return login(email, PASSWORD);
    }

    private long createMovie(String title, String price, int stock) {
        Reply reply = api().post("/api/peliculas", """
                {"titulo":"%s","anio":1979,"genero":"Science fiction","director":"Ridley Scott","precio":%s,"stock":%d}"""
                .formatted(title, price, stock), adminToken);
        assertEquals(201, reply.status(), reply.body());
        return ((Number) reply.json("$.id")).longValue();
    }

    @Test
    void initialAdminFromConfigurationCanLogIn() {
        api();

        assertEquals(3, adminToken.split("\\.").length, "a JWT has three parts");
    }

    @Test
    void customerJourneyFromBrowsingToPurchaseHistory() {
        api();
        long movie = createMovie("E2E Alien " + UUID.randomUUID(), "12.99", 5);
        String user = newUserToken();

        // The catalog is public
        Reply browse = api.get("/api/peliculas/" + movie, null);
        assertEquals(200, browse.status());
        assertEquals(12.99, ((Number) browse.json("$.precio")).doubleValue());

        // Cart: add, no duplicates, view with live price
        Reply added = api.post("/api/carrito/lineas", "{\"peliculaId\":%d}".formatted(movie), user);
        assertEquals(201, added.status(), added.body());
        assertEquals(409, api.post("/api/carrito/lineas", "{\"peliculaId\":%d}".formatted(movie), user).status());
        Reply cart = api.get("/api/carrito", user);
        assertEquals(12.99, ((Number) cart.json("$.total")).doubleValue());

        // Rating and favourites
        Reply rated = api.put("/api/peliculas/%d/valoracion".formatted(movie), "{\"puntuacion\":5,\"comentario\":\"Great\"}", user);
        assertEquals(200, rated.status(), rated.body());
        assertEquals(204, api.put("/api/peliculas/%d/favorita".formatted(movie), null, user).status());
        List<Integer> favorites = api.get("/api/usuarios/me/favoritas", user).json("$[*].id");
        assertTrue(favorites.contains((int) movie));
        assertEquals(5.0, ((Number) api.get("/api/peliculas/" + movie, null).json("$.valoracionMedia")).doubleValue());

        // Checkout freezes the price, empties the cart and shows up in the history
        Reply bought = api.post("/api/compras", null, user);
        assertEquals(201, bought.status(), bought.body());
        long purchaseId = ((Number) bought.json("$.id")).longValue();
        assertEquals(0, ((List<?>) api.get("/api/carrito", user).json("$.lineas")).size());

        api.put("/api/peliculas/" + movie,
                "{\"titulo\":\"Renamed\",\"anio\":1979,\"genero\":\"Drama\",\"director\":\"X\",\"precio\":99.00}", adminToken);
        Reply detail = api.get("/api/compras/" + purchaseId, user);
        assertEquals(200, detail.status());
        assertEquals(12.99, ((Number) detail.json("$.total")).doubleValue());
        assertEquals(1, ((List<?>) api.get("/api/compras", user).json("$")).size());

        // Checking out an empty cart is a conflict
        assertEquals(409, api.post("/api/compras", null, user).status());
    }

    @Test
    void purchasesAreStrictlyPrivate() {
        api();
        long movie = createMovie("E2E Private " + UUID.randomUUID(), "5.00", 1);
        String owner = newUserToken();
        String other = newUserToken();
        api.post("/api/carrito/lineas", "{\"peliculaId\":%d}".formatted(movie), owner);
        long purchaseId = ((Number) api.post("/api/compras", null, owner).json("$.id")).longValue();

        assertEquals(404, api.get("/api/compras/" + purchaseId, other).status());
        assertEquals(0, ((List<?>) api.get("/api/compras", other).json("$")).size());
    }

    @Test
    void orderLifecycleReservesAndReleasesStock() {
        api();
        long movie = createMovie("E2E Order " + UUID.randomUUID(), "10.00", 3);
        String user = newUserToken();

        Reply created = api.post("/api/orders",
                "{\"userId\":1,\"lines\":[{\"productId\":%d,\"quantity\":2}]}".formatted(movie), user);
        assertEquals(201, created.status(), created.body());
        long orderId = ((Number) created.json("$.id")).longValue();
        assertEquals(20.0, ((Number) created.json("$.total")).doubleValue());
        assertEquals(1, ((Number) api.get("/api/peliculas/" + movie, null).json("$.stock")).intValue());

        // Not enough stock left: conflict and nothing is reserved
        assertEquals(409, api.post("/api/orders",
                "{\"userId\":1,\"lines\":[{\"productId\":%d,\"quantity\":2}]}".formatted(movie), user).status());
        assertEquals(1, ((Number) api.get("/api/peliculas/" + movie, null).json("$.stock")).intValue());

        // Cancel returns the stock once; a second cancel is a conflict
        Reply cancelled = api.post("/api/orders/%d/cancel".formatted(orderId), null, user);
        assertEquals(200, cancelled.status(), cancelled.body());
        assertEquals("CANCELLED", cancelled.json("$.status"));
        assertEquals(3, ((Number) api.get("/api/peliculas/" + movie, null).json("$.stock")).intValue());
        assertEquals(409, api.post("/api/orders/%d/cancel".formatted(orderId), null, user).status());
        assertEquals(404, api.get("/api/orders/999999", user).status());
    }

    @Test
    void securityRulesHoldOverRealHttp() {
        api();
        String user = newUserToken();

        assertEquals(401, api.get("/api/carrito", null).status());
        assertEquals(401, api.post("/api/compras", null, null).status());
        assertEquals(403, api.post("/api/peliculas",
                "{\"titulo\":\"X\",\"anio\":2000,\"genero\":\"Drama\",\"director\":\"Y\",\"precio\":1.00}", user).status());
        assertEquals(401, api.post("/api/auth/login",
                "{\"email\":\"nadie@correo.com\",\"contrasena\":\"incorrecta1\"}", null).status());
        assertEquals(409, api.post("/api/auth/registro",
                "{\"email\":\"%s\",\"contrasena\":\"%s\"}".formatted(ADMIN_EMAIL, PASSWORD), null).status());
    }
}

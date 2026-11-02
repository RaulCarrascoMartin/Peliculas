package shop.compra.internal.web;

import com.jayway.jsonpath.JsonPath;
import shop.compartido.seguridad.JwtTokens;
import shop.compra.internal.domain.Compra;
import shop.compra.internal.domain.CompraRepository;
import shop.compra.internal.domain.LineaCompraRepository;
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
 * Doble checkout simultáneo del mismo carrito por la capa HTTP real (sin dobles): verifica el
 * mecanismo de verdad —{@code @Version} de {@code Carrito} + transacción + handler 409—, no la
 * conversión de la excepción. Se repite varias rondas con una barrera para forzar el solape; sea
 * cual sea el entrelazado, el resultado correcto es el mismo: una compra y un 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CheckoutConcurrenteIntegracionTest {

    private static final int RONDAS = 25;
    private static final long PRIMER_USUARIO = 4001;
    private static final String ALIEN = """
            {"titulo":"Alien","anio":1979,"genero":"Ciencia ficción","director":"Ridley Scott","precio":12.99}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @Autowired
    private CompraRepository compras;

    @Autowired
    private LineaCompraRepository lineas;

    private final List<Long> usuariosUsados = new ArrayList<>();
    private final List<Long> peliculasCreadas = new ArrayList<>();

    @AfterEach
    void limpiar() throws Exception {
        for (long usuarioId : usuariosUsados) {
            for (Compra compra : compras.findByUsuarioIdOrderByFechaDesc(usuarioId)) {
                lineas.deleteAll(lineas.findByCompraIdOrderById(compra.getId()));
                compras.delete(compra);
            }
            mvc.perform(delete("/api/carrito").header("Authorization", bearer(usuarioId)));
        }
        for (long id : peliculasCreadas) {
            mvc.perform(delete("/api/peliculas/" + id).header("Authorization", bearerAdmin()));
        }
    }

    private String bearer(long usuarioId) {
        if (!usuariosUsados.contains(usuarioId)) {
            usuariosUsados.add(usuarioId);
        }
        return "Bearer " + tokens.emitir(usuarioId, "USUARIO");
    }

    private String bearerAdmin() {
        return "Bearer " + tokens.emitir(1L, "ADMIN");
    }

    private long crearPelicula() throws Exception {
        String cuerpo = mvc.perform(post("/api/peliculas").header("Authorization", bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(ALIEN))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
        peliculasCreadas.add(id);
        return id;
    }

    private int finalizar(long usuarioId) throws Exception {
        return mvc.perform(post("/api/compras").header("Authorization", bearer(usuarioId)))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void dosCheckoutsSimultaneosCreanUnaSolaCompraYElOtroRecibe409() throws Exception {
        long pelicula = crearPelicula();
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            for (int ronda = 0; ronda < RONDAS; ronda++) {
                long usuario = PRIMER_USUARIO + ronda;
                mvc.perform(post("/api/carrito/lineas").header("Authorization", bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"peliculaId\":" + pelicula + "}"));

                var barrera = new CyclicBarrier(2);
                Callable<Integer> checkout = () -> {
                    barrera.await();
                    return finalizar(usuario);
                };
                Future<Integer> a = hilos.submit(checkout);
                Future<Integer> b = hilos.submit(checkout);
                List<Integer> estados = List.of(a.get(), b.get()).stream().sorted().toList();

                assertEquals(List.of(201, 409), estados, "ronda " + ronda + ": una compra y un conflicto");
                assertEquals(1, compras.findByUsuarioIdOrderByFechaDesc(usuario).size(),
                        "ronda " + ronda + ": exactamente una compra");
                String carrito = mvc.perform(get("/api/carrito").header("Authorization", bearer(usuario)))
                        .andReturn().getResponse().getContentAsString();
                assertTrue(((List<?>) JsonPath.read(carrito, "$.lineas")).isEmpty(),
                        "ronda " + ronda + ": carrito vacío al final");
            }
        } finally {
            hilos.shutdownNow();
        }
    }
}

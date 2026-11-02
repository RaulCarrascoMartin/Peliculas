package org.example.compra.internal.web;

import com.jayway.jsonpath.JsonPath;
import org.example.compartido.JwtTokens;
import org.example.compra.internal.domain.Compra;
import org.example.compra.internal.domain.CompraRepository;
import org.example.compra.internal.domain.LineaCompraRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CompraControllerIntegracionTest {

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

    private final List<Long> peliculasCreadas = new ArrayList<>();
    private final List<Long> usuariosUsados = new ArrayList<>();

    /** La BD se comparte entre pruebas: se borran las compras de los usuarios usados, sus carritos y las películas. */
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
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
        peliculasCreadas.add(id);
        return id;
    }

    private void anadirAlCarrito(long usuarioId, long peliculaId) throws Exception {
        mvc.perform(post("/api/carrito/lineas").header("Authorization", bearer(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"peliculaId\":" + peliculaId + "}"))
                .andExpect(status().isCreated());
    }

    private ResultActions finalizar(long usuarioId) throws Exception {
        return mvc.perform(post("/api/compras").header("Authorization", bearer(usuarioId)));
    }

    private ResultActions detalle(long usuarioId, long compraId) throws Exception {
        return mvc.perform(get("/api/compras/" + compraId).header("Authorization", bearer(usuarioId)));
    }

    private long idDe(ResultActions resultado) throws Exception {
        String cuerpo = resultado.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    @Test
    void finalizarDevuelve201ConElSnapshotYVaciaElCarrito() throws Exception {
        long pelicula = crearPelicula();
        anadirAlCarrito(3001, pelicula);

        finalizar(3001)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("COMPRADA"))
                .andExpect(jsonPath("$.fecha", containsString("T")))
                .andExpect(jsonPath("$.lineas", hasSize(1)))
                .andExpect(jsonPath("$.lineas[0].peliculaId").value(pelicula))
                .andExpect(jsonPath("$.lineas[0].titulo").value("Alien"))
                .andExpect(jsonPath("$.lineas[0].precioUnitario").value(12.99))
                .andExpect(jsonPath("$.total").value(12.99));

        mvc.perform(get("/api/carrito").header("Authorization", bearer(3001)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas").isEmpty());
    }

    @Test
    void elPrecioDeLaCompraNoCambiaSiLaPeliculaSeEditaDespues() throws Exception {
        long pelicula = crearPelicula();
        anadirAlCarrito(3002, pelicula);
        long compra = idDe(finalizar(3002).andExpect(status().isCreated()));

        String editada = """
                {"titulo":"Alien 2","anio":1979,"genero":"Ciencia ficción","director":"Ridley Scott","precio":99.00}""";
        mvc.perform(put("/api/peliculas/" + pelicula).header("Authorization", bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(editada))
                .andExpect(status().isOk());

        detalle(3002, compra)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas[0].titulo").value("Alien"))
                .andExpect(jsonPath("$.lineas[0].precioUnitario").value(12.99))
                .andExpect(jsonPath("$.total").value(12.99));
    }

    @Test
    void finalizarConElCarritoVacioDevuelve409ConElCuerpoDeError() throws Exception {
        finalizar(3003)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.errores").isMap());
    }

    @Test
    void unaCompraAjenaOInexistenteDevuelve404NuncaElCuerpoNi403() throws Exception {
        long pelicula = crearPelicula();
        anadirAlCarrito(3004, pelicula);
        long compra = idDe(finalizar(3004).andExpect(status().isCreated()));

        detalle(3005, compra)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
        detalle(3004, 99999999L).andExpect(status().isNotFound());
    }

    @Test
    void listarDevuelveSoloLasComprasPropiasDeLaMasRecienteALaMasAntigua() throws Exception {
        long pelicula = crearPelicula();
        anadirAlCarrito(3006, pelicula);
        long primera = idDe(finalizar(3006).andExpect(status().isCreated()));
        Thread.sleep(10);
        anadirAlCarrito(3006, pelicula);
        long segunda = idDe(finalizar(3006).andExpect(status().isCreated()));
        anadirAlCarrito(3007, pelicula);
        finalizar(3007).andExpect(status().isCreated());

        mvc.perform(get("/api/compras").header("Authorization", bearer(3006)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(segunda))
                .andExpect(jsonPath("$[1].id").value(primera))
                .andExpect(jsonPath("$[0].lineas[0].titulo").value("Alien"));
    }

    @Test
    void sinTokenLasTresRutasDevuelven401() throws Exception {
        mvc.perform(post("/api/compras")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/compras")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/compras/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void niSiquieraElAdministradorVeLaCompraDeOtroUsuario() throws Exception {
        long pelicula = crearPelicula();
        anadirAlCarrito(3008, pelicula);
        long compra = idDe(finalizar(3008).andExpect(status().isCreated()));

        mvc.perform(get("/api/compras/" + compra).header("Authorization", bearerAdmin()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/compras").header("Authorization", bearerAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + compra + ")]").isEmpty());
    }

    @Test
    void unIdentificadorDeUsuarioEnLaPeticionSeIgnoraYSeUsaElDelToken() throws Exception {
        long pelicula = crearPelicula();
        anadirAlCarrito(3009, pelicula);

        mvc.perform(post("/api/compras?usuarioId=3009").header("Authorization", bearer(3010)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/compras?usuarioId=3009").header("Authorization", bearer(3010)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/carrito").header("Authorization", bearer(3009)))
                .andExpect(jsonPath("$.lineas", hasSize(1)));
    }
}

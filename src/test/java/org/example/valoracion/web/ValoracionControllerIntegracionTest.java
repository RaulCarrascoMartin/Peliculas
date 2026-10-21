package org.example.valoracion.web;

import com.jayway.jsonpath.JsonPath;
import org.example.compartido.JwtTokens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ValoracionControllerIntegracionTest {

    private static final String ALIEN = """
            {"titulo":"Alien","anio":1979,"genero":"Ciencia ficción","director":"Ridley Scott"}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    private final List<Long> peliculasCreadas = new ArrayList<>();

    /** La base de datos se comparte entre pruebas: se borran las películas para no alterar otros listados. */
    @AfterEach
    void limpiar() throws Exception {
        for (long id : peliculasCreadas) {
            mvc.perform(delete("/api/peliculas/" + id).header("Authorization", bearer(1, "ADMIN")));
        }
    }

    private String bearer(long usuarioId, String rol) {
        return "Bearer " + tokens.emitir(usuarioId, rol);
    }

    private long crearPelicula() throws Exception {
        String cuerpo = mvc.perform(post("/api/peliculas").header("Authorization", bearer(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(ALIEN))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
        peliculasCreadas.add(id);
        return id;
    }

    private ResultActions valorar(long peliculaId, String autorizacion, String json) throws Exception {
        MockHttpServletRequestBuilder peticion = put("/api/peliculas/" + peliculaId + "/valoracion")
                .contentType(MediaType.APPLICATION_JSON).content(json);
        if (autorizacion != null) {
            peticion.header("Authorization", autorizacion);
        }
        return mvc.perform(peticion);
    }

    @Test
    void unUsuarioValoraUnaPeliculaYRecibeSuValoracion() throws Exception {
        long pelicula = crearPelicula();

        valorar(pelicula, bearer(100, "USUARIO"), "{\"puntuacion\":4,\"comentario\":\"Muy buena\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.peliculaId").value(pelicula))
                .andExpect(jsonPath("$.puntuacion").value(4))
                .andExpect(jsonPath("$.comentario").value("Muy buena"));
    }

    @Test
    void valorarDeNuevoActualizaLaValoracionYLaMediaSeVeEnElDetalle() throws Exception {
        long pelicula = crearPelicula();
        valorar(pelicula, bearer(101, "USUARIO"), "{\"puntuacion\":5}").andExpect(status().isOk());
        String primera = valorar(pelicula, bearer(102, "USUARIO"), "{\"puntuacion\":1}")
                .andReturn().getResponse().getContentAsString();

        valorar(pelicula, bearer(102, "USUARIO"), "{\"puntuacion\":3,\"comentario\":\"Mejor\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(((Number) JsonPath.read(primera, "$.id")).longValue()))
                .andExpect(jsonPath("$.puntuacion").value(3));

        mvc.perform(get("/api/peliculas/" + pelicula))
                .andExpect(jsonPath("$.valoracionMedia").value(4.0));
    }

    @Test
    void unaPuntuacionFueraDeRangoOAusenteDevuelve400() throws Exception {
        long pelicula = crearPelicula();

        valorar(pelicula, bearer(103, "USUARIO"), "{\"puntuacion\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.puntuacion").isNotEmpty());
        valorar(pelicula, bearer(103, "USUARIO"), "{\"puntuacion\":6}").andExpect(status().isBadRequest());
        valorar(pelicula, bearer(103, "USUARIO"), "{\"comentario\":\"sin nota\"}")
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/peliculas/" + pelicula)).andExpect(jsonPath("$.valoracionMedia").isEmpty());
    }

    @Test
    void valorarUnaPeliculaInexistenteDevuelve404() throws Exception {
        valorar(987654, bearer(104, "USUARIO"), "{\"puntuacion\":3}").andExpect(status().isNotFound());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        long pelicula = crearPelicula();

        valorar(pelicula, null, "{\"puntuacion\":3}").andExpect(status().isUnauthorized());
    }
}

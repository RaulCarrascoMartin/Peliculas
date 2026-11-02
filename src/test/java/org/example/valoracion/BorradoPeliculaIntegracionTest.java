package org.example.valoracion;

import com.jayway.jsonpath.JsonPath;
import org.example.compartido.seguridad.JwtTokens;
import org.example.valoracion.internal.domain.MarcaPeliculaRepository;
import org.example.valoracion.internal.domain.ValoracionRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Borrar una película no toca los datos de otras. Sin eventos entre módulos, sus valoraciones y marcas no se
 * borran (no hay ciclos pelicula↔valoracion), pero las listas ya no la muestran.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BorradoPeliculaIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @Autowired
    private ValoracionRepository valoraciones;

    @Autowired
    private MarcaPeliculaRepository marcas;

    private final List<Long> peliculasCreadas = new ArrayList<>();

    @AfterEach
    void limpiar() throws Exception {
        for (long id : peliculasCreadas) {
            mvc.perform(delete("/api/peliculas/" + id).header("Authorization", bearer(1, "ADMIN")));
        }
    }

    private String bearer(long usuarioId, String rol) {
        return "Bearer " + tokens.emitir(usuarioId, rol);
    }

    private long crearPelicula(String titulo) throws Exception {
        String json = "{\"titulo\":\"" + titulo + "\",\"anio\":1979,\"genero\":\"Drama\",\"director\":\"X\",\"precio\":12.99}";
        String cuerpo = mvc.perform(post("/api/peliculas").header("Authorization", bearer(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
        peliculasCreadas.add(id);
        return id;
    }

    private ResultActions valorar(long peliculaId, long usuarioId, int puntuacion) throws Exception {
        return mvc.perform(put("/api/peliculas/" + peliculaId + "/valoracion")
                .header("Authorization", bearer(usuarioId, "USUARIO"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"puntuacion\":" + puntuacion + "}"));
    }

    private ResultActions marcar(long peliculaId, long usuarioId, String lista) throws Exception {
        return mvc.perform(put("/api/peliculas/" + peliculaId + "/" + lista)
                .header("Authorization", bearer(usuarioId, "USUARIO")));
    }

    @Test
    void unaPeliculaEliminadaDejaDeAparecerEnLasListasAunqueLasMarcasNoSeBorren() throws Exception {
        long id = crearPelicula("Borrado Uno");
        marcar(id, 300, "favorita").andExpect(status().isNoContent());
        marcar(id, 300, "vista").andExpect(status().isNoContent());
        mvc.perform(get("/api/usuarios/me/favoritas").header("Authorization", bearer(300, "USUARIO")))
                .andExpect(jsonPath("$[0].id").value(id));

        mvc.perform(delete("/api/peliculas/" + id).header("Authorization", bearer(1, "ADMIN")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/peliculas/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/usuarios/me/favoritas").header("Authorization", bearer(300, "USUARIO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/usuarios/me/vistas").header("Authorization", bearer(300, "USUARIO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void eliminarUnaPeliculaNoTocaLosDatosDeOtras() throws Exception {
        long borrada = crearPelicula("Borrado Dos");
        long conservada = crearPelicula("Borrado Tres");
        valorar(borrada, 302, 5).andExpect(status().isOk());
        valorar(conservada, 302, 3).andExpect(status().isOk());
        marcar(borrada, 302, "favorita").andExpect(status().isNoContent());
        marcar(conservada, 302, "favorita").andExpect(status().isNoContent());

        mvc.perform(delete("/api/peliculas/" + borrada).header("Authorization", bearer(1, "ADMIN")))
                .andExpect(status().isNoContent());

        assertEquals(1, valoraciones.findByPeliculaId(conservada).size());
        var favoritas = marcas.findByUsuarioIdAndTipo(302L, org.example.valoracion.internal.domain.TipoMarca.FAVORITA);
        assertTrue(favoritas.stream().anyMatch(m -> m.getPeliculaId().equals(conservada)));
        mvc.perform(get("/api/usuarios/me/favoritas").header("Authorization", bearer(302, "USUARIO")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(conservada));
    }

    @Test
    void eliminarUnaPeliculaInexistenteDevuelve404YNoTocaNada() throws Exception {
        long id = crearPelicula("Borrado Cuatro");
        valorar(id, 303, 4).andExpect(status().isOk());

        mvc.perform(delete("/api/peliculas/987654").header("Authorization", bearer(1, "ADMIN")))
                .andExpect(status().isNotFound());

        assertEquals(1, valoraciones.findByPeliculaId(id).size());
    }
}

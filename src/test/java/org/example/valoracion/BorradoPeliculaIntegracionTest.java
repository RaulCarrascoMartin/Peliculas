package org.example.valoracion;

import com.jayway.jsonpath.JsonPath;
import org.example.compartido.JwtTokens;
import org.example.valoracion.domain.MarcaPeliculaRepository;
import org.example.valoracion.domain.ValoracionRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Borrar una película elimina sus valoraciones y marcas (requisito 1.4), de extremo a extremo. */
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
    void eliminarUnaPeliculaBorraSusValoracionesYMarcas() throws Exception {
        long id = crearPelicula("Borrado Uno");
        valorar(id, 300, 4).andExpect(status().isOk());
        valorar(id, 301, 2).andExpect(status().isOk());
        marcar(id, 300, "favorita").andExpect(status().isNoContent());
        marcar(id, 300, "vista").andExpect(status().isNoContent());
        marcar(id, 301, "vista").andExpect(status().isNoContent());
        assertEquals(2, valoraciones.findByPeliculaId(id).size());

        mvc.perform(delete("/api/peliculas/" + id).header("Authorization", bearer(1, "ADMIN")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/peliculas/" + id)).andExpect(status().isNotFound());
        assertEquals(0, valoraciones.findByPeliculaId(id).size());
        assertEquals(0, marcas.findByUsuarioIdAndTipo(300L, org.example.valoracion.domain.TipoMarca.FAVORITA).size());
        assertEquals(0, marcas.findByUsuarioIdAndTipo(300L, org.example.valoracion.domain.TipoMarca.VISTA).size());
        assertEquals(0, marcas.findByUsuarioIdAndTipo(301L, org.example.valoracion.domain.TipoMarca.VISTA).size());
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
        var favoritas = marcas.findByUsuarioIdAndTipo(302L, org.example.valoracion.domain.TipoMarca.FAVORITA);
        assertEquals(1, favoritas.size());
        assertEquals(conservada, favoritas.get(0).getPeliculaId());
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

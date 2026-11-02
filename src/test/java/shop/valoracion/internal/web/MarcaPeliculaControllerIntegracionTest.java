package shop.valoracion.internal.web;

import com.jayway.jsonpath.JsonPath;
import shop.compartido.seguridad.JwtTokens;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MarcaPeliculaControllerIntegracionTest {

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

    private long crearPelicula(String titulo) throws Exception {
        String json = "{\"titulo\":\"" + titulo + "\",\"anio\":1979,\"genero\":\"Drama\",\"director\":\"X\",\"precio\":12.99}";
        String cuerpo = mvc.perform(post("/api/peliculas").header("Authorization", bearer(1, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
        peliculasCreadas.add(id);
        return id;
    }

    private ResultActions marcar(String ruta, long peliculaId, long usuarioId) throws Exception {
        return mvc.perform(put("/api/peliculas/" + peliculaId + "/" + ruta).header("Authorization", bearer(usuarioId, "USUARIO")));
    }

    private ResultActions desmarcar(String ruta, long peliculaId, long usuarioId) throws Exception {
        return mvc.perform(delete("/api/peliculas/" + peliculaId + "/" + ruta).header("Authorization", bearer(usuarioId, "USUARIO")));
    }

    private ResultActions lista(String ruta, long usuarioId) throws Exception {
        return mvc.perform(get("/api/usuarios/me/" + ruta).header("Authorization", bearer(usuarioId, "USUARIO")));
    }

    @Test
    void marcarComoFavoritaLaAnadeAMisFavoritasConSusDatos() throws Exception {
        long id = crearPelicula("Marca Uno");

        marcar("favorita", id, 200).andExpect(status().isNoContent());

        lista("favoritas", 200)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].titulo").value("Marca Uno"))
                .andExpect(jsonPath("$[0].valoracionMedia").isEmpty());
        lista("vistas", 200).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void marcarComoVistaYDesmarcarLaQuitaDeLaLista() throws Exception {
        long id = crearPelicula("Marca Dos");
        marcar("vista", id, 201).andExpect(status().isNoContent());
        marcar("vista", id, 201).andExpect(status().isNoContent());
        lista("vistas", 201).andExpect(jsonPath("$.length()").value(1));

        desmarcar("vista", id, 201).andExpect(status().isNoContent());

        lista("vistas", 201).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cadaUsuarioSoloVeSusPropiasListas() throws Exception {
        long id = crearPelicula("Marca Tres");
        marcar("favorita", id, 202).andExpect(status().isNoContent());

        lista("favoritas", 203).andExpect(jsonPath("$.length()").value(0));
        desmarcar("favorita", id, 203).andExpect(status().isNoContent());
        lista("favoritas", 202).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void marcarUnaPeliculaInexistenteDevuelve404() throws Exception {
        marcar("favorita", 987654, 204).andExpect(status().isNotFound());
        marcar("vista", 987654, 204).andExpect(status().isNotFound());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(put("/api/peliculas/1/favorita")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/peliculas/1/vista")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me/favoritas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me/vistas")).andExpect(status().isUnauthorized());
    }

    @Test
    void desmarcarSinMarcaPreviaDevuelve204() throws Exception {
        long id = crearPelicula("Marca Cuatro");

        desmarcar("favorita", id, 205).andExpect(status().isNoContent());
        lista("favoritas", 205).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unaPeliculaPuedeEstarEnFavoritasYVistasALaVez() throws Exception {
        long id = crearPelicula("Marca Cinco");

        marcar("favorita", id, 206).andExpect(status().isNoContent());
        marcar("vista", id, 206).andExpect(status().isNoContent());
        desmarcar("favorita", id, 206).andExpect(status().isNoContent());

        lista("favoritas", 206).andExpect(jsonPath("$.length()").value(0));
        lista("vistas", 206).andExpect(jsonPath("$.length()").value(1));
    }
}

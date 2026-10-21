package org.example.pelicula.web;

import com.jayway.jsonpath.JsonPath;
import org.example.compartido.JwtTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PeliculaControllerIntegracionTest {

    private static final String ALIEN = """
            {"titulo":"Alien","anio":1979,"genero":"Ciencia ficción","director":"Ridley Scott"}""";
    private static final String ALIENS = """
            {"titulo":"Aliens","anio":1986,"genero":"Acción","director":"James Cameron"}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    private String admin() {
        return "Bearer " + tokens.emitir(1L, "ADMIN");
    }

    private String usuario() {
        return "Bearer " + tokens.emitir(2L, "USUARIO");
    }

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion,
                                 String autorizacion, String json) throws Exception {
        if (autorizacion != null) {
            peticion.header("Authorization", autorizacion);
        }
        return mvc.perform(peticion.contentType(MediaType.APPLICATION_JSON).content(json == null ? "" : json));
    }

    private long crearComoAdmin(String json) throws Exception {
        String cuerpo = enviar(post("/api/peliculas"), admin(), json)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    @Test
    void elAdministradorCreaUnaPeliculaYRecibeSuIdentificador() throws Exception {
        enviar(post("/api/peliculas"), admin(), ALIEN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titulo").value("Alien"))
                .andExpect(jsonPath("$.anio").value(1979))
                .andExpect(jsonPath("$.genero").value("Ciencia ficción"))
                .andExpect(jsonPath("$.director").value("Ridley Scott"))
                .andExpect(jsonPath("$.valoracionMedia").isEmpty());
    }

    @Test
    void sinTituloOConAnioInvalidoDevuelve400() throws Exception {
        enviar(post("/api/peliculas"), admin(),
                "{\"titulo\":\"\",\"anio\":5,\"genero\":\"Drama\",\"director\":\"X\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.titulo").isNotEmpty())
                .andExpect(jsonPath("$.errores.anio").isNotEmpty());
    }

    @Test
    void sinGeneroNiDirectorDevuelve400() throws Exception {
        enviar(post("/api/peliculas"), admin(), "{\"titulo\":\"Alien\",\"anio\":1979}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.genero").isNotEmpty())
                .andExpect(jsonPath("$.errores.director").isNotEmpty());
    }

    @Test
    void elDetalleEsPublicoYMuestraLaValoracionMedia() throws Exception {
        long id = crearComoAdmin(ALIEN);

        mvc.perform(get("/api/peliculas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Alien"))
                .andExpect(jsonPath("$.valoracionMedia").isEmpty());
    }

    @Test
    void detalleDeUnaPeliculaInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/peliculas/987654"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404));
    }

    @Test
    void unIdentificadorNoNumericoDevuelve400() throws Exception {
        mvc.perform(get("/api/peliculas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));
    }

    @Test
    void elAdministradorEditaLaPelicula() throws Exception {
        long id = crearComoAdmin(ALIEN);

        enviar(put("/api/peliculas/" + id), admin(), ALIENS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Aliens"))
                .andExpect(jsonPath("$.anio").value(1986));
    }

    @Test
    void editarUnaPeliculaInexistenteDevuelve404() throws Exception {
        enviar(put("/api/peliculas/987654"), admin(), ALIENS).andExpect(status().isNotFound());
    }

    @Test
    void editarConDatosInvalidosDevuelve400() throws Exception {
        long id = crearComoAdmin(ALIEN);

        enviar(put("/api/peliculas/" + id), admin(), "{\"titulo\":\"\"}").andExpect(status().isBadRequest());
    }

    @Test
    void elAdministradorEliminaLaPelicula() throws Exception {
        long id = crearComoAdmin(ALIEN);

        enviar(delete("/api/peliculas/" + id), admin(), null).andExpect(status().isNoContent());

        mvc.perform(get("/api/peliculas/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnaPeliculaInexistenteDevuelve404() throws Exception {
        enviar(delete("/api/peliculas/987654"), admin(), null).andExpect(status().isNotFound());
    }

    @Test
    void unUsuarioSinRolAdminNoPuedeCrearEditarNiEliminar() throws Exception {
        long id = crearComoAdmin(ALIEN);

        enviar(post("/api/peliculas"), usuario(), ALIEN).andExpect(status().isForbidden());
        enviar(put("/api/peliculas/" + id), usuario(), ALIENS).andExpect(status().isForbidden());
        enviar(delete("/api/peliculas/" + id), usuario(), null).andExpect(status().isForbidden());

        mvc.perform(get("/api/peliculas/" + id)).andExpect(jsonPath("$.titulo").value("Alien"));
    }

    @Test
    void sinTokenNoSePuedeCrearEditarNiEliminar() throws Exception {
        enviar(post("/api/peliculas"), null, ALIEN).andExpect(status().isUnauthorized());
        enviar(put("/api/peliculas/1"), null, ALIENS).andExpect(status().isUnauthorized());
        enviar(delete("/api/peliculas/1"), null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void validaLosLimitesDelAnio() throws Exception {
        enviar(post("/api/peliculas"), admin(), conAnio(1887)).andExpect(status().isBadRequest());
        enviar(post("/api/peliculas"), admin(), conAnio(2101)).andExpect(status().isBadRequest());
        enviar(post("/api/peliculas"), admin(), conAnio(1888)).andExpect(status().isCreated());
        enviar(post("/api/peliculas"), admin(), conAnio(2100)).andExpect(status().isCreated());
    }

    @Test
    void sinAnioDevuelve400() throws Exception {
        enviar(post("/api/peliculas"), admin(), "{\"titulo\":\"Alien\",\"genero\":\"Drama\",\"director\":\"X\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.anio").isNotEmpty());
    }

    @Test
    void unaEdicionInvalidaNoModificaLaPelicula() throws Exception {
        long id = crearComoAdmin(ALIEN);

        enviar(put("/api/peliculas/" + id), admin(), conAnio(5)).andExpect(status().isBadRequest());

        mvc.perform(get("/api/peliculas/" + id))
                .andExpect(jsonPath("$.titulo").value("Alien"))
                .andExpect(jsonPath("$.anio").value(1979));
    }

    private static String conAnio(int anio) {
        return "{\"titulo\":\"Alien\",\"anio\":" + anio + ",\"genero\":\"Drama\",\"director\":\"X\"}";
    }

    @Test
    void elListadoEsPublicoYPaginado() throws Exception {
        for (int i = 0; i < 3; i++) {
            crearComoAdmin(ALIEN);
        }

        mvc.perform(get("/api/peliculas").param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].titulo").value("Alien"))
                .andExpect(jsonPath("$.content[0].valoracionMedia").isEmpty());

        mvc.perform(get("/api/peliculas").param("size", "2"))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").isNumber());
    }

    @Test
    void unaBusquedaSinResultadosDevuelveListaVaciaConEstado200() throws Exception {
        crearComoAdmin(ALIEN);

        mvc.perform(get("/api/peliculas").param("titulo", "no-existe-xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0));

        mvc.perform(get("/api/peliculas").param("titulo", "alien").param("genero", "Western"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}

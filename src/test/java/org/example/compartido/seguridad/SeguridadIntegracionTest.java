package org.example.compartido.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SeguridadIntegracionTest.ControladorProtegido.class)
class SeguridadIntegracionTest {

    @RestController
    static class ControladorProtegido {
        private final UsuarioActual usuarioActual;

        ControladorProtegido(UsuarioActual usuarioActual) {
            this.usuarioActual = usuarioActual;
        }

        @GetMapping("/api/protegido")
        String protegido() {
            return "usuario " + usuarioActual.id();
        }

        @GetMapping("/api/administracion")
        @PreAuthorize("hasRole('ADMIN')")
        String administracion() {
            return "admin " + usuarioActual.id();
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @Value("${peliculas.jwt.secreto}")
    private String secreto;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void rutaProtegidaSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/protegido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void tokenValidoPermiteAccederYIdentificaAlUsuario() throws Exception {
        mvc.perform(get("/api/protegido").header("Authorization", bearer(tokens.emitir(7L, "USUARIO"))))
                .andExpect(status().isOk())
                .andExpect(content().string("usuario 7"));
    }

    @Test
    void tokenCaducadoDevuelve401() throws Exception {
        String caducado = new JwtTokens(secreto, Duration.ofSeconds(-10)).emitir(7L, "USUARIO");

        mvc.perform(get("/api/protegido").header("Authorization", bearer(caducado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenConFirmaAlteradaDevuelve401() throws Exception {
        String token = tokens.emitir(7L, "USUARIO");
        String alterado = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        mvc.perform(get("/api/protegido").header("Authorization", bearer(alterado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFirmadoConOtroSecretoDevuelve401() throws Exception {
        String ajeno = new JwtTokens("otro-secreto-distinto-con-32-bytes-o-mas!!", Duration.ofHours(1))
                .emitir(7L, "ADMIN");

        mvc.perform(get("/api/protegido").header("Authorization", bearer(ajeno)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cabeceraSinEsquemaBearerDevuelve401() throws Exception {
        mvc.perform(get("/api/protegido").header("Authorization", tokens.emitir(7L, "USUARIO")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolInsuficienteDevuelve403() throws Exception {
        mvc.perform(get("/api/administracion").header("Authorization", bearer(tokens.emitir(7L, "USUARIO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));
    }

    @Test
    void administradorAccedeALaRutaDeAdministracion() throws Exception {
        mvc.perform(get("/api/administracion").header("Authorization", bearer(tokens.emitir(1L, "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().string("admin 1"));
    }

    @Test
    void registroYLoginSonPublicosYElTokenObtenidoSirve() throws Exception {
        String credenciales = "{\"email\":\"ana@correo.com\",\"contrasena\":\"Secreta123\"}";
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(credenciales))
                .andExpect(status().isCreated());

        String respuesta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = respuesta.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mvc.perform(get("/api/protegido").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void tokenQueNoEsUnJwtDevuelve401() throws Exception {
        mvc.perform(get("/api/protegido").header("Authorization", bearer("esto-no-es-un-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void registroDuplicadoDevuelve409ConLaSeguridadReal() throws Exception {
        String credenciales = "{\"email\":\"duplicado@correo.com\",\"contrasena\":\"Secreta123\"}";
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(credenciales))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(credenciales))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409));
    }

    @Test
    void loginIncorrectoDevuelve401SinIndicarQueCampoFallo() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@correo.com\",\"contrasena\":\"Secreta123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
    }

    /** Las siete rutas de carrito y compras (requisito 7.1). */
    private record Ruta(HttpMethod metodo, String url) {
    }

    private static final List<Ruta> RUTAS_DE_CARRITO_Y_COMPRAS = List.of(
            new Ruta(HttpMethod.GET, "/api/carrito"),
            new Ruta(HttpMethod.POST, "/api/carrito/lineas"),
            new Ruta(HttpMethod.DELETE, "/api/carrito/lineas/1"),
            new Ruta(HttpMethod.DELETE, "/api/carrito"),
            new Ruta(HttpMethod.POST, "/api/compras"),
            new Ruta(HttpMethod.GET, "/api/compras"),
            new Ruta(HttpMethod.GET, "/api/compras/1"));

    private void comprobar401(Ruta ruta, String cabecera) throws Exception {
        var peticion = request(ruta.metodo(), ruta.url())
                .contentType(MediaType.APPLICATION_JSON).content("{\"peliculaId\":1}");
        if (cabecera != null) {
            peticion.header("Authorization", cabecera);
        }
        mvc.perform(peticion)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void lasSieteRutasDeCarritoYComprasDevuelven401SinToken() throws Exception {
        for (Ruta ruta : RUTAS_DE_CARRITO_Y_COMPRAS) {
            comprobar401(ruta, null);
        }
    }

    @Test
    void lasSieteRutasDeCarritoYComprasDevuelven401ConTokenCaducado() throws Exception {
        String caducado = new JwtTokens(secreto, Duration.ofSeconds(-10)).emitir(7L, "USUARIO");
        for (Ruta ruta : RUTAS_DE_CARRITO_Y_COMPRAS) {
            comprobar401(ruta, bearer(caducado));
        }
    }

    @Test
    void lasSieteRutasDeCarritoYComprasDevuelven401ConTokenInvalido() throws Exception {
        String ajeno = new JwtTokens("otro-secreto-distinto-con-32-bytes-o-mas!!", Duration.ofHours(1))
                .emitir(7L, "USUARIO");
        for (Ruta ruta : RUTAS_DE_CARRITO_Y_COMPRAS) {
            comprobar401(ruta, bearer("esto-no-es-un-jwt"));
            comprobar401(ruta, bearer(ajeno));
        }
    }

    @Test
    void losContratosPublicadosNoDeclaranNingunIdentificadorDeUsuarioEnRutasNiCuerpos() throws Exception {
        for (String contrato : List.of("/contratos/carrito-api.yml", "/contratos/compras-api.yml")) {
            String texto = mvc.perform(get(contrato)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertFalse(texto.contains("usuarioId"), contrato + " declara usuarioId");
            assertFalse(texto.contains("usuario_id"), contrato + " declara usuario_id");
            assertFalse(texto.contains("{usuario"), contrato + " declara un parámetro de ruta de usuario");
        }
    }
}

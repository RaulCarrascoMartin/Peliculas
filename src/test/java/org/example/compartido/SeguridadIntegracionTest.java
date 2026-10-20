package org.example.compartido;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}

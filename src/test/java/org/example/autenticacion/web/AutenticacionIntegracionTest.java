package org.example.autenticacion.web;

import com.jayway.jsonpath.JsonPath;
import org.example.autenticacion.domain.Rol;
import org.example.autenticacion.domain.UsuarioRepository;
import org.example.compartido.JwtTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Requisito 0 con la aplicación completa: cuenta con rol de usuario, contraseña cifrada y token con id y rol. */
@SpringBootTest
@AutoConfigureMockMvc
class AutenticacionIntegracionTest {

    private static final String EMAIL = "integracion.auth@correo.com";
    private static final String CONTRASENA = "Secreta123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private JwtTokens tokens;

    private static String credenciales() {
        return "{\"email\":\"" + EMAIL + "\",\"contrasena\":\"" + CONTRASENA + "\"}";
    }

    @Test
    void elRegistroCreaUnaCuentaConRolUsuarioYContrasenaCifrada() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(credenciales()))
                .andExpect(status().isCreated());

        var usuario = usuarios.findByEmail(EMAIL).orElseThrow();
        assertEquals(Rol.USUARIO, usuario.getRol());
        assertNotEquals(CONTRASENA, usuario.getContrasena());
        assertTrue(usuario.getContrasena().startsWith("$2"), "debe ser un hash BCrypt");
    }

    @Test
    void elLoginDevuelveUnTokenConElIdentificadorYElRolDelUsuario() throws Exception {
        if (usuarios.findByEmail(EMAIL).isEmpty()) {
            mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(credenciales()))
                    .andExpect(status().isCreated());
        }

        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        var datos = tokens.leer(JsonPath.read(cuerpo, "$.token"));
        assertEquals(usuarios.findByEmail(EMAIL).orElseThrow().getId(), datos.id());
        assertEquals("USUARIO", datos.rol());
    }
}

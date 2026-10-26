package org.example.compartido;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El front de pruebas se sirve sin token; el resto de rutas siguen protegidas. */
@SpringBootTest
@AutoConfigureMockMvc
class FrontEstaticoIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void laPaginaPrincipalYSusRecursosSonPublicos() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<title>")));
        mvc.perform(get("/app.js")).andExpect(status().isOk());
        mvc.perform(get("/estilos.css")).andExpect(status().isOk());
    }

    @Test
    void laPaginaDelContratoYLosYmlSonPublicos() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SwaggerUIBundle")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/contratos/carrito-api.yml")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/contratos/compras-api.yml")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("persistAuthorization: true")));
        mvc.perform(get("/contratos/carrito-api.yml")).andExpect(status().isOk());
        mvc.perform(get("/contratos/compras-api.yml")).andExpect(status().isOk());
    }

    @Test
    void losRecursosDelWebjarDeSwaggerSonPublicos() throws Exception {
        mvc.perform(get("/webjars/swagger-ui/" + versionWebjarSwaggerUi() + "/swagger-ui-bundle.js"))
                .andExpect(status().isOk());
        mvc.perform(get("/webjars/swagger-ui/" + versionWebjarSwaggerUi() + "/swagger-ui.css"))
                .andExpect(status().isOk());
    }

    private String versionWebjarSwaggerUi() {
        var props = new java.util.Properties();
        try (var is = getClass().getResourceAsStream("/META-INF/maven/org.webjars/swagger-ui/pom.properties")) {
            props.load(is);
            return props.getProperty("version");
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se encontró la versión del webjar swagger-ui", e);
        }
    }

    @Test
    void otrasRutasSiguenProtegidas() throws Exception {
        mvc.perform(get("/api/usuarios/me/favoritas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/otra-cosa.js")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/carrito")).andExpect(status().isUnauthorized());
    }
}

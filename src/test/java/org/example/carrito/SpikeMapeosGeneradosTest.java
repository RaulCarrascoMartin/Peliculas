package org.example.carrito;

import org.example.carrito.web.api.CarritoRestApi;
import org.example.compra.web.api.ComprasRestApi;
import org.example.carrito.web.dto.LineaCarrito;
import org.example.compra.web.dto.LineaCompra;
import org.example.compartido.JwtTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El plugin genera las interfaces del contrato (SPIKE 1.1): Spring MVC debe
 * registrar los mapeos que heredan los controladores que las implementen.
 * Se elimina cuando existan los controladores reales de carrito y compra (tareas 4.4 y 5.3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SpikeMapeosGeneradosTest.ControladorSpike.class)
class SpikeMapeosGeneradosTest {

    @RestController
    static class ControladorSpike implements CarritoRestApi, ComprasRestApi {
        @Override
        public LineaCarrito verCarrito() {
            return new LineaCarrito(1L, "Alien");
        }

        @Override
        public LineaCompra listarCompras() {
            return new LineaCompra(2L, "Jaws");
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @Test
    void elMapeoDeLaInterfaceGeneradaSeRegistraYResponde() throws Exception {
        mvc.perform(get("/api/carrito").header("Authorization", "Bearer " + tokens.emitir(7L, "USUARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peliculaId").value(1))
                .andExpect(jsonPath("$.titulo").value("Alien"));

        mvc.perform(get("/api/compras").header("Authorization", "Bearer " + tokens.emitir(7L, "USUARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peliculaId").value(2))
                .andExpect(jsonPath("$.titulo").value("Jaws"));
    }

    @Test
    void laRutaGeneradaEsProtegidaSinToken() throws Exception {
        mvc.perform(get("/api/carrito")).andExpect(status().isUnauthorized());
    }
}

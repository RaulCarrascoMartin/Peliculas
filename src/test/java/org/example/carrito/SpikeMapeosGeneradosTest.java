package org.example.carrito;

import org.example.carrito.web.api.CarritoRestApi;
import org.example.carrito.web.dto.AnadirLinea;
import org.example.carrito.web.dto.Carrito;
import org.example.carrito.web.dto.LineaCarrito;
import org.example.compra.web.api.ComprasRestApi;
import org.example.compra.web.dto.Compra;
import org.example.compra.web.dto.LineaCompra;
import org.example.compartido.JwtTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba SPIKE 1.1 (llevada a los contratos definitivos en 2.3): Spring MVC
 * registra los mapeos que los controladores heredan de las interfaces generadas,
 * con los 200/201/204 declarados en el contrato.
 * Se elimina cuando existan los controladores reales de carrito y compra (tareas 4.4 y 5.3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SpikeMapeosGeneradosTest.ControladorSpike.class)
class SpikeMapeosGeneradosTest {

    @RestController
    static class ControladorSpike implements CarritoRestApi, ComprasRestApi {
        @Override
        public Carrito verCarrito() {
            return new Carrito(List.of(new LineaCarrito(1L, "Alien", new BigDecimal("12.99"))),
                    new BigDecimal("12.99"));
        }

        @Override
        public LineaCarrito anadirLinea(AnadirLinea anadirLinea) {
            return new LineaCarrito(anadirLinea.peliculaId(), "Alien", new BigDecimal("12.99"));
        }

        @Override
        public void quitarLinea(Long peliculaId) {
        }

        @Override
        public void vaciarCarrito() {
        }

        @Override
        public Compra finalizarCompra() {
            return new Compra(10L, OffsetDateTime.now(), Compra.EstadoEnum.COMPRADA,
                    List.of(new LineaCompra(1L, "Alien", new BigDecimal("12.99"))), new BigDecimal("12.99"));
        }

        @Override
        public List<Compra> listarCompras() {
            return List.of(finalizarCompra());
        }

        @Override
        public Compra detalleCompra(Long compraId) {
            return finalizarCompra();
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokens tokens;

    @Test
    void losMapeosDeLasInterfacesGeneradasSeRegistranYResponden() throws Exception {
        var cabecera = java.util.Map.entry("Authorization", "Bearer " + tokens.emitir(7L, "USUARIO"));

        mvc.perform(get("/api/carrito").header(cabecera.getKey(), cabecera.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(12.99))
                .andExpect(jsonPath("$.lineas", hasSize(1)))
                .andExpect(jsonPath("$.lineas[0].titulo").value("Alien"));

        mvc.perform(post("/api/carrito/lineas").header(cabecera.getKey(), cabecera.getValue())
                        .contentType("application/json").content("{\"peliculaId\": 1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precio").value(12.99));
    }

    @Test
    void lasComprasSeRegistranDesdeSuInterfaceGenerada() throws Exception {
        mvc.perform(get("/api/compras").header("Authorization", "Bearer " + tokens.emitir(7L, "USUARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estado").value("COMPRADA"))
                .andExpect(jsonPath("$[0].total").value(12.99));
    }

    @Test
    void lasRutasGeneradasSigueProtegidasSinToken() throws Exception {
        mvc.perform(get("/api/carrito")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/compras")).andExpect(status().isUnauthorized());
    }
}

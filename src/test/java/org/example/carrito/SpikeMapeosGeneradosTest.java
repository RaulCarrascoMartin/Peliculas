package org.example.carrito;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba SPIKE 1.1 (llevada a los contratos definitivos en 2.3): Spring MVC
 * registra los mapeos que los controladores heredan de las interfaces generadas,
 * con los 200/201/204 declarados en el contrato.
 * La mitad de carrito se retiró en la tarea 4.4 (el controlador real la cubre);
 * queda la de compra y se elimina al llegar la tarea 5.3.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SpikeMapeosGeneradosTest.ControladorSpike.class)
class SpikeMapeosGeneradosTest {

    @RestController
    static class ControladorSpike implements ComprasRestApi {

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
    void lasComprasSeRegistranDesdeSuInterfaceGenerada() throws Exception {
        mvc.perform(get("/api/compras").header("Authorization", "Bearer " + tokens.emitir(7L, "USUARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estado").value("COMPRADA"))
                .andExpect(jsonPath("$[0].total").value(12.99));
    }

    @Test
    void laRutaGeneradaDeComprasSigueProtegidaSinToken() throws Exception {
        mvc.perform(postSinToken()).andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postSinToken() {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/compras");
    }
}

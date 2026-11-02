package shop.compartido.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Comprueba, de extremo a extremo por HTTP, la tabla de errores del diseño. */
class ManejoErroresMvcTest {

    record EntradaDePrueba(@NotBlank String titulo, @NotNull Integer anio) {
    }

    @RestController
    static class ControladorDePrueba {
        @PostMapping("/validacion")
        void validar(@Valid @RequestBody EntradaDePrueba entrada) {
        }

        @GetMapping("/no-encontrado")
        void noEncontrado() {
            throw new RecursoNoEncontradoException("Película 7 no encontrada");
        }

        @GetMapping("/conflicto")
        void conflicto() {
            throw new ConflictoException("El email ya está registrado");
        }

        @GetMapping("/credenciales")
        void credenciales() {
            throw new BadCredentialsException("la contraseña no coincide para a@b.c");
        }

        @GetMapping("/pagina")
        void pagina(@RequestParam @Min(0) int numero) {
        }

        @GetMapping("/inesperado")
        void inesperado() {
            throw new IllegalStateException("detalle interno que no debe filtrarse");
        }

        @GetMapping("/prohibido")
        void prohibido() {
            throw new AccessDeniedException("solo administradores");
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validacionFallidaDevuelve400ConErroresPorCampo() throws Exception {
        mvc.perform(post("/validacion").contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.errores.titulo").isNotEmpty())
                .andExpect(jsonPath("$.errores.anio").isNotEmpty());
    }

    @Test
    void peticionValidaNoGeneraError() throws Exception {
        mvc.perform(post("/validacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Alien\",\"anio\":1979}"))
                .andExpect(status().isOk());
    }

    @Test
    void recursoInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje").value("Película 7 no encontrada"))
                .andExpect(jsonPath("$.errores").isEmpty());
    }

    @Test
    void conflictoDevuelve409() throws Exception {
        mvc.perform(get("/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").value("El email ya está registrado"));
    }

    @Test
    void credencialesIncorrectasDevuelven401SinRevelarElMotivo() throws Exception {
        mvc.perform(get("/credenciales"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
    }

    @Test
    void rolInsuficienteDevuelve403() throws Exception {
        mvc.perform(get("/prohibido"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.mensaje").value("Acceso denegado"));
    }

    @Test
    void jsonMalFormadoDevuelve400Uniforme() throws Exception {
        mvc.perform(post("/validacion").contentType(MediaType.APPLICATION_JSON).content("{titulo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.errores").isEmpty());
    }

    @Test
    void validacionDeParametrosDevuelve400ConErroresPorCampo() throws Exception {
        mvc.perform(get("/pagina").param("numero", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.errores.numero").isNotEmpty());
    }

    @Test
    void metodoNoPermitidoDevuelve405Uniforme() throws Exception {
        mvc.perform(post("/conflicto"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.estado").value(405))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void errorInesperadoDevuelve500SinFiltrarDetalles() throws Exception {
        mvc.perform(get("/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.estado").value(500))
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor"));
    }
}

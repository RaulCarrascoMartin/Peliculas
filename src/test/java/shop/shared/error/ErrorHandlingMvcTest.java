package shop.shared.error;

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

/** Checks, end to end over HTTP, the design's error table. */
class ErrorHandlingMvcTest {

    record TestInput(@NotBlank String title, @NotNull Integer year) {
    }

    @RestController
    static class TestController {
        @PostMapping("/validacion")
        void validate(@Valid @RequestBody TestInput input) {
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Película 7 no encontrada");
        }

        @GetMapping("/conflicto")
        void conflict() {
            throw new ConflictException("El email ya está registrado");
        }

        @GetMapping("/credenciales")
        void credentials() {
            throw new BadCredentialsException("password does not match for a@b.c");
        }

        @GetMapping("/page")
        void page(@RequestParam @Min(0) int numero) {
        }

        @GetMapping("/inesperado")
        void unexpected() {
            throw new IllegalStateException("internal detail that must not leak");
        }

        @GetMapping("/prohibido")
        void forbidden() {
            throw new AccessDeniedException("admins only");
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void failedValidationReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/validacion").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.errores.title").isNotEmpty())
                .andExpect(jsonPath("$.errores.year").isNotEmpty());
    }

    @Test
    void validRequestProducesNoError() throws Exception {
        mvc.perform(post("/validacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Alien\",\"year\":1979}"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownResourceReturns404() throws Exception {
        mvc.perform(get("/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje").value("Película 7 no encontrada"))
                .andExpect(jsonPath("$.errores").isEmpty());
    }

    @Test
    void conflictReturns409() throws Exception {
        mvc.perform(get("/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").value("El email ya está registrado"));
    }

    @Test
    void badCredentialsReturn401WithoutRevealingReason() throws Exception {
        mvc.perform(get("/credenciales"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
    }

    @Test
    void insufficientRoleReturns403() throws Exception {
        mvc.perform(get("/prohibido"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.mensaje").value("Acceso denegado"));
    }

    @Test
    void malformedJsonReturnsUniform400() throws Exception {
        mvc.perform(post("/validacion").contentType(MediaType.APPLICATION_JSON).content("{title"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.errores").isEmpty());
    }

    @Test
    void parameterValidationReturns400WithFieldErrors() throws Exception {
        mvc.perform(get("/page").param("numero", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.errores.numero").isNotEmpty());
    }

    @Test
    void methodNotAllowedReturnsUniform405() throws Exception {
        mvc.perform(post("/conflicto"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.estado").value(405))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        mvc.perform(get("/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.estado").value(500))
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor"));
    }
}

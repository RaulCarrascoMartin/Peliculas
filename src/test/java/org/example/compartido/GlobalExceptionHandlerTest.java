package org.example.compartido;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void recursoNoEncontradoDevuelve404() {
        var respuesta = handler.recursoNoEncontrado(new RecursoNoEncontradoException("Película 7 no encontrada"));

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals(404, respuesta.getBody().estado());
        assertEquals("Película 7 no encontrada", respuesta.getBody().mensaje());
        assertTrue(respuesta.getBody().errores().isEmpty());
    }

    @Test
    void conflictoDevuelve409() {
        var respuesta = handler.conflicto(new ConflictoException("El email ya está registrado"));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals(409, respuesta.getBody().estado());
        assertEquals("El email ya está registrado", respuesta.getBody().mensaje());
    }

    @Test
    void validacionDevuelve400ConErroresPorCampo() throws Exception {
        var resultado = new BeanPropertyBindingResult(new Object(), "peliculaRequest");
        resultado.addError(new FieldError("peliculaRequest", "titulo", "no debe estar vacío"));
        resultado.addError(new FieldError("peliculaRequest", "anio", "año inválido"));
        var ex = new MethodArgumentNotValidException(
                new MethodParameter(Object.class.getMethod("toString"), -1), resultado);

        var respuesta = handler.validacion(ex);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals(400, respuesta.getBody().estado());
        assertNotNull(respuesta.getBody().mensaje());
        assertEquals("no debe estar vacío", respuesta.getBody().errores().get("titulo"));
        assertEquals("año inválido", respuesta.getBody().errores().get("anio"));
    }

    @Test
    void credencialesIncorrectasDevuelven401SinDetalle() {
        var respuesta = handler.noAutenticado(new BadCredentialsException("contraseña errónea para a@b.c"));

        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        assertEquals(401, respuesta.getBody().estado());
        assertEquals("Credenciales incorrectas", respuesta.getBody().mensaje());
    }

    @Test
    void accesoDenegadoDevuelve403() {
        var respuesta = handler.accesoDenegado(new AccessDeniedException("denegado"));

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        assertEquals(403, respuesta.getBody().estado());
        assertEquals("Acceso denegado", respuesta.getBody().mensaje());
    }
}

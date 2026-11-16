package shop.shared.error;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void resourceNotFoundReturns404() {
        var response = handler.resourceNotFound(new ResourceNotFoundException("Película 7 no encontrada"));

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        assertEquals("Película 7 no encontrada", response.getDetail());
        assertNull(response.getProperties());
    }

    @Test
    void conflictReturns409() {
        var response = handler.conflict(new ConflictException("El email ya está registrado"));

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
        assertEquals("El email ya está registrado", response.getDetail());
    }

    @Test
    void businessValidationReturns400() {
        var response = handler.validation(new BusinessValidationException("La puntuación debe estar entre 1 y 5"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals("La puntuación debe estar entre 1 y 5", response.getDetail());
    }

    @Test
    void validationReturns400WithFieldErrors() throws Exception {
        var result = new BeanPropertyBindingResult(new Object(), "peliculaRequest");
        result.addError(new FieldError("peliculaRequest", "titulo", "must not be empty"));
        result.addError(new FieldError("peliculaRequest", "anio", "invalid year"));
        var ex = new MethodArgumentNotValidException(
                new MethodParameter(Object.class.getMethod("toString"), -1), result);

        var response = handler.validation(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertNotNull(response.getDetail());
        assertEquals(List.of("titulo: must not be empty", "anio: invalid year"),
                response.getProperties().get("errors"));
    }

    @Test
    void badCredentialsReturn401WithoutDetail() {
        var response = handler.unauthenticated(new BadCredentialsException("wrong password for a@b.c"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals("Credenciales incorrectas", response.getDetail());
    }

    @Test
    void accessDeniedReturns403() {
        var response = handler.accessDenied(new AccessDeniedException("denied"));

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertEquals("Acceso denegado", response.getDetail());
    }

    @Test
    void optimisticLockReturns409WithStandardBodyWhereverItComesFrom() {
        var response = handler.optimisticLock(new OptimisticLockingFailureException("Cart 7 was updated by another transaction"));

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
        assertNotNull(response.getDetail());
        assertNull(response.getProperties());
    }

    @Test
    void optimisticLockDoesNotLeakInternalDetailsInMessage() {
        var response = handler.optimisticLock(
                new OptimisticLockingFailureException("Row was updated or deleted by another transaction: Carrito#7"));

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
        assertTrue(!response.getDetail().contains("Carrito#7"));
    }
}

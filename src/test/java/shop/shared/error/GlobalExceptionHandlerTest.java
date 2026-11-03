package shop.shared.error;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
    void resourceNotFoundReturns404() {
        var response = handler.resourceNotFound(new ResourceNotFoundException("Película 7 no encontrada"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("Película 7 no encontrada", response.getBody().message());
        assertTrue(response.getBody().errors().isEmpty());
    }

    @Test
    void conflictReturns409() {
        var response = handler.conflict(new ConflictException("El email ya está registrado"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertEquals("El email ya está registrado", response.getBody().message());
    }

    @Test
    void businessValidationReturns400() {
        var response = handler.validation(new BusinessValidationException("La puntuación debe estar entre 1 y 5"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
        assertEquals("La puntuación debe estar entre 1 y 5", response.getBody().message());
    }

    @Test
    void validationReturns400WithFieldErrors() throws Exception {
        var result = new BeanPropertyBindingResult(new Object(), "peliculaRequest");
        result.addError(new FieldError("peliculaRequest", "titulo", "must not be empty"));
        result.addError(new FieldError("peliculaRequest", "anio", "invalid year"));
        var ex = new MethodArgumentNotValidException(
                new MethodParameter(Object.class.getMethod("toString"), -1), result);

        var response = handler.validation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
        assertNotNull(response.getBody().message());
        assertEquals("must not be empty", response.getBody().errors().get("titulo"));
        assertEquals("invalid year", response.getBody().errors().get("anio"));
    }

    @Test
    void badCredentialsReturn401WithoutDetail() {
        var response = handler.unauthenticated(new BadCredentialsException("wrong password for a@b.c"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
        assertEquals("Credenciales incorrectas", response.getBody().message());
    }

    @Test
    void accessDeniedReturns403() {
        var response = handler.accessDenied(new AccessDeniedException("denied"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().status());
        assertEquals("Acceso denegado", response.getBody().message());
    }

    @Test
    void optimisticLockReturns409WithStandardBodyWhereverItComesFrom() {
        var response = handler.optimisticLock(new ObjectOptimisticLockingFailureException("Cart", 7L));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertNotNull(response.getBody().message());
        assertTrue(response.getBody().errors().isEmpty());
    }

    @Test
    void optimisticLockDoesNotLeakInternalDetailsInMessage() {
        var response = handler.optimisticLock(
                new OptimisticLockingFailureException("Row was updated or deleted by another transaction: Carrito#7"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(!response.getBody().message().contains("Carrito#7"));
    }
}

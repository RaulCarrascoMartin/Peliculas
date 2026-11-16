package shop.shared.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BusinessExceptionsTest {

    @Test
    void resourceNotFoundKeepsMessage() {
        var ex = new ResourceNotFoundException("Película 7 no encontrada");

        assertInstanceOf(RuntimeException.class, ex);
        assertEquals("Película 7 no encontrada", ex.getMessage());
    }

    @Test
    void conflictKeepsMessage() {
        var ex = new ConflictException("El email ya está registrado");

        assertInstanceOf(RuntimeException.class, ex);
        assertEquals("El email ya está registrado", ex.getMessage());
    }

    @Test
    void validationKeepsMessage() {
        var ex = new BusinessValidationException("La puntuación debe estar entre 1 y 5");

        assertInstanceOf(RuntimeException.class, ex);
        assertEquals("La puntuación debe estar entre 1 y 5", ex.getMessage());
    }
}

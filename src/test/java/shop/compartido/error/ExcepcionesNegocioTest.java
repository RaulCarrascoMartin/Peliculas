package shop.compartido.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ExcepcionesNegocioTest {

    @Test
    void recursoNoEncontradoConservaElMensaje() {
        var ex = new RecursoNoEncontradoException("Película 7 no encontrada");

        assertInstanceOf(RuntimeException.class, ex);
        assertEquals("Película 7 no encontrada", ex.getMessage());
    }

    @Test
    void conflictoConservaElMensaje() {
        var ex = new ConflictoException("El email ya está registrado");

        assertInstanceOf(RuntimeException.class, ex);
        assertEquals("El email ya está registrado", ex.getMessage());
    }

    @Test
    void validacionConservaElMensaje() {
        var ex = new ValidacionException("La puntuación debe estar entre 1 y 5");

        assertInstanceOf(RuntimeException.class, ex);
        assertEquals("La puntuación debe estar entre 1 y 5", ex.getMessage());
    }
}

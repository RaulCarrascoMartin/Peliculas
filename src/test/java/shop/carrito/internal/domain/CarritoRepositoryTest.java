package shop.carrito.internal.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class CarritoRepositoryTest {

    @Autowired
    private CarritoRepository repositorio;

    private Carrito carrito(Long usuarioId) {
        var carrito = new Carrito();
        carrito.setUsuarioId(usuarioId);
        return carrito;
    }

    @Test
    void guardaElCarritoYAsignaId() {
        assertNotNull(repositorio.saveAndFlush(carrito(1L)).getId());
    }

    @Test
    void buscaElCarritoDeUnUsuario() {
        repositorio.saveAndFlush(carrito(1L));
        repositorio.saveAndFlush(carrito(2L));

        var propio = repositorio.findByUsuarioId(1L).orElseThrow();

        assertEquals(1L, propio.getUsuarioId());
        assertTrue(repositorio.findByUsuarioId(3L).isEmpty());
    }

    @Test
    void noPermiteDosCarritosDelMismoUsuario() {
        repositorio.saveAndFlush(carrito(1L));

        assertThrows(DataIntegrityViolationException.class, () -> repositorio.saveAndFlush(carrito(1L)));
    }

    @Test
    void elUsuarioEsObligatorio() {
        assertThrows(DataIntegrityViolationException.class, () -> repositorio.saveAndFlush(carrito(null)));
    }

    @Test
    void laVersionArrancaYCambiaAlActualizar() {
        var guardado = repositorio.saveAndFlush(carrito(1L));
        assertEquals(0L, guardado.getVersion());

        guardado.setUsuarioId(2L);
        repositorio.saveAndFlush(guardado);

        assertEquals(1L, guardado.getVersion());
    }
}

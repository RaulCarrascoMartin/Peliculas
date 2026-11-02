package org.example.carrito.internal.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class LineaCarritoRepositoryTest {

    @Autowired
    private LineaCarritoRepository repositorio;

    private LineaCarrito linea(Long carritoId, Long peliculaId) {
        var linea = new LineaCarrito();
        linea.setCarritoId(carritoId);
        linea.setPeliculaId(peliculaId);
        return linea;
    }

    @Test
    void guardaLaLineaYAsignaId() {
        var guardada = repositorio.saveAndFlush(linea(1L, 10L));

        assertEquals(10L, guardada.getPeliculaId());
        assertEquals(1L, guardada.getCarritoId());
    }

    @Test
    void buscaLaLineaDeUnaPeliculaEnUnCarrito() {
        repositorio.saveAndFlush(linea(1L, 10L));
        repositorio.saveAndFlush(linea(1L, 11L));
        repositorio.saveAndFlush(linea(2L, 10L));

        assertEquals(10L, repositorio.findByCarritoIdAndPeliculaId(1L, 10L).orElseThrow().getPeliculaId());
        assertTrue(repositorio.findByCarritoIdAndPeliculaId(1L, 12L).isEmpty());
    }

    @Test
    void listaLasLineasDeUnCarritoPorOrdenDeInsercion() {
        repositorio.saveAndFlush(linea(1L, 12L));
        repositorio.saveAndFlush(linea(1L, 10L));
        repositorio.saveAndFlush(linea(2L, 99L));

        List<Long> peliculas = repositorio.findByCarritoIdOrderById(1L).stream()
                .map(LineaCarrito::getPeliculaId).toList();

        assertEquals(List.of(12L, 10L), peliculas);
    }

    @Test
    void noPermiteDosLineasDeLaMismaPeliculaEnElMismoCarrito() {
        repositorio.saveAndFlush(linea(1L, 10L));

        assertThrows(DataIntegrityViolationException.class, () -> repositorio.saveAndFlush(linea(1L, 10L)));
    }

    @Test
    void laMismaPeliculaPuedeEstarEnCarritosDistintos() {
        repositorio.saveAndFlush(linea(1L, 10L));
        repositorio.saveAndFlush(linea(2L, 10L));

        assertEquals(2, repositorio.count());
    }

    @Test
    void borraTodasLasLineasDeUnaPeliculaEnTodosLosCarritos() {
        repositorio.saveAndFlush(linea(1L, 10L));
        repositorio.saveAndFlush(linea(2L, 10L));
        repositorio.saveAndFlush(linea(1L, 11L));

        repositorio.deleteByPeliculaId(10L);

        assertEquals(1, repositorio.count());
        assertTrue(repositorio.findByCarritoIdAndPeliculaId(1L, 10L).isEmpty());
        assertTrue(repositorio.findByCarritoIdAndPeliculaId(2L, 10L).isEmpty());
    }

    @Test
    void borraTodasLasLineasDeUnCarrito() {
        repositorio.saveAndFlush(linea(1L, 10L));
        repositorio.saveAndFlush(linea(1L, 11L));
        repositorio.saveAndFlush(linea(2L, 12L));

        repositorio.deleteByCarritoId(1L);

        assertEquals(1, repositorio.count());
        assertTrue(repositorio.findByCarritoIdAndPeliculaId(1L, 10L).isEmpty());
        assertTrue(repositorio.findByCarritoIdAndPeliculaId(1L, 11L).isEmpty());
    }
}

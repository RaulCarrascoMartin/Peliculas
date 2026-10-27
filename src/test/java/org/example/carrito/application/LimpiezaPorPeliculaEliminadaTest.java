package org.example.carrito.application;

import org.example.carrito.domain.LineaCarrito;
import org.example.pelicula.PeliculaEliminada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LimpiezaPorPeliculaEliminadaTest {

    private LineaCarritoRepositoryFalso lineas;
    private LimpiezaPorPeliculaEliminada limpieza;

    @BeforeEach
    void preparar() {
        lineas = new LineaCarritoRepositoryFalso();
        limpieza = new LimpiezaPorPeliculaEliminada(lineas);
    }

    private void linea(Long carritoId, Long peliculaId) {
        var linea = new LineaCarrito();
        linea.setCarritoId(carritoId);
        linea.setPeliculaId(peliculaId);
        lineas.save(linea);
    }

    @Test
    void borraLasLineasDeLaPeliculaEliminadaEnTodosLosCarritos() {
        linea(1L, 10L);
        linea(2L, 10L);
        linea(1L, 11L);

        limpieza.alEliminarPelicula(new PeliculaEliminada(10L));

        List<Long> restantes = lineas.findByCarritoIdOrderById(1L).stream()
                .map(LineaCarrito::getPeliculaId).toList();
        assertEquals(List.of(11L), restantes);
        assertEquals(1, lineas.count());
    }

    @Test
    void laMismaPeliculaEnVariosCarritosSeEliminaCompleta() {
        linea(1L, 10L);
        linea(2L, 10L);

        limpieza.alEliminarPelicula(new PeliculaEliminada(10L));

        assertEquals(0, lineas.count());
    }

    @Test
    void sinLineasAsociadasNoHaceNada() {
        linea(1L, 11L);

        limpieza.alEliminarPelicula(new PeliculaEliminada(10L));

        assertEquals(1, lineas.count());
    }
}

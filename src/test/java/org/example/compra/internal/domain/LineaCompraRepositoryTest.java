package org.example.compra.internal.domain;

import org.example.pelicula.internal.domain.Pelicula;
import org.example.pelicula.internal.domain.PeliculaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class LineaCompraRepositoryTest {

    @Autowired
    private LineaCompraRepository lineas;

    @Autowired
    private PeliculaRepository peliculas;

    private LineaCompra linea(Long compraId, Long peliculaId, String titulo, String precio) {
        var linea = new LineaCompra();
        linea.setCompraId(compraId);
        linea.setPeliculaId(peliculaId);
        linea.setTitulo(titulo);
        linea.setPrecioUnitario(new BigDecimal(precio));
        return linea;
    }

    @Test
    void guardaLaLineaConSuSnapshotDeTituloYPrecio() {
        var guardada = lineas.saveAndFlush(linea(1L, 10L, "Matrix", "7.99"));

        var leida = lineas.findById(guardada.getId()).orElseThrow();
        assertEquals("Matrix", leida.getTitulo());
        assertEquals(new BigDecimal("7.99"), leida.getPrecioUnitario());
        assertEquals(10L, leida.getPeliculaId());
    }

    @Test
    void listaLasLineasDeUnaCompraPorOrdenDeInsercion() {
        lineas.saveAndFlush(linea(1L, 12L, "Origen", "9.49"));
        lineas.saveAndFlush(linea(1L, 10L, "Matrix", "7.99"));
        lineas.saveAndFlush(linea(2L, 99L, "Otra", "1.00"));

        List<String> titulos = lineas.findByCompraIdOrderById(1L).stream().map(LineaCompra::getTitulo).toList();

        assertEquals(List.of("Origen", "Matrix"), titulos);
    }

    @Test
    void elHistorialConservaTituloYPrecioAunqueLaPeliculaSeEditeOSeElimine() {
        var pelicula = new Pelicula();
        pelicula.setTitulo("Matrix");
        pelicula.setPrecio(new BigDecimal("7.99"));
        var guardada = peliculas.saveAndFlush(pelicula);
        lineas.saveAndFlush(linea(1L, guardada.getId(), guardada.getTitulo(), "7.99"));

        guardada.setTitulo("Matrix Reloaded");
        guardada.setPrecio(new BigDecimal("14.99"));
        peliculas.saveAndFlush(guardada);

        var trasEditar = lineas.findByCompraIdOrderById(1L).get(0);
        assertEquals("Matrix", trasEditar.getTitulo());
        assertEquals(new BigDecimal("7.99"), trasEditar.getPrecioUnitario());

        peliculas.deleteById(guardada.getId());
        peliculas.flush();

        var trasEliminar = lineas.findByCompraIdOrderById(1L);
        assertEquals(1, trasEliminar.size());
        assertEquals("Matrix", trasEliminar.get(0).getTitulo());
        assertEquals(new BigDecimal("7.99"), trasEliminar.get(0).getPrecioUnitario());
        assertTrue(peliculas.findById(guardada.getId()).isEmpty());
    }
}

package org.example.valoracion.internal.usecase;

import org.example.pelicula.api.PeliculaEliminada;
import org.example.valoracion.internal.domain.MarcaPelicula;
import org.example.valoracion.internal.domain.TipoMarca;
import org.example.valoracion.internal.domain.Valoracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimpiezaPorPeliculaEliminadaTest {

    private ValoracionRepositoryFalso valoraciones;
    private MarcaPeliculaRepositoryFalso marcas;
    private LimpiezaPorPeliculaEliminada limpieza;

    @BeforeEach
    void preparar() {
        valoraciones = new ValoracionRepositoryFalso();
        marcas = new MarcaPeliculaRepositoryFalso();
        limpieza = new LimpiezaPorPeliculaEliminada(valoraciones, marcas);
    }

    private void valoracion(Long usuarioId, Long peliculaId) {
        var valoracion = new Valoracion();
        valoracion.setUsuarioId(usuarioId);
        valoracion.setPeliculaId(peliculaId);
        valoracion.setPuntuacion(4);
        valoraciones.save(valoracion);
    }

    private void marca(Long usuarioId, Long peliculaId, TipoMarca tipo) {
        var marca = new MarcaPelicula();
        marca.setUsuarioId(usuarioId);
        marca.setPeliculaId(peliculaId);
        marca.setTipo(tipo);
        marcas.save(marca);
    }

    @Test
    void borraLasValoracionesYMarcasDeLaPeliculaEliminada() {
        valoracion(1L, 10L);
        valoracion(2L, 10L);
        marca(1L, 10L, TipoMarca.FAVORITA);
        marca(1L, 10L, TipoMarca.VISTA);

        limpieza.alEliminarPelicula(new PeliculaEliminada(10L));

        assertTrue(valoraciones.findByPeliculaId(10L).isEmpty());
        assertEquals(0, marcas.count());
    }

    @Test
    void noTocaLasDeOtrasPeliculas() {
        valoracion(1L, 10L);
        valoracion(1L, 11L);
        marca(1L, 10L, TipoMarca.FAVORITA);
        marca(1L, 11L, TipoMarca.FAVORITA);

        limpieza.alEliminarPelicula(new PeliculaEliminada(10L));

        assertEquals(1, valoraciones.count());
        assertEquals(11L, valoraciones.findAll().get(0).getPeliculaId());
        assertEquals(1, marcas.count());
        assertEquals(11L, marcas.findAll().get(0).getPeliculaId());
    }

    @Test
    void sinDatosAsociadosNoHaceNada() {
        limpieza.alEliminarPelicula(new PeliculaEliminada(10L));

        assertEquals(0, valoraciones.count());
    }
}

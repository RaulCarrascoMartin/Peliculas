package org.example.valoracion.application;

import org.example.compartido.RecursoNoEncontradoException;
import org.example.compartido.UsuarioActualFalso;
import org.example.compartido.ValidacionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValoracionServiceTest {

    private static final Long PELICULA = 10L;

    private ValoracionRepositoryFalso repositorio;
    private PeliculaApiFalsa peliculas;

    @BeforeEach
    void preparar() {
        repositorio = new ValoracionRepositoryFalso();
        peliculas = new PeliculaApiFalsa();
        peliculas.anadirPelicula(PELICULA);
    }

    private ValoracionService servicioDe(Long usuarioId) {
        return new ValoracionService(repositorio, peliculas, new UsuarioActualFalso(usuarioId, "USUARIO"));
    }

    @Test
    void guardaLaValoracionDelUsuarioActual() {
        var valoracion = servicioDe(1L).valorar(PELICULA, 4, "Muy buena");

        assertNotNull(valoracion.getId());
        assertEquals(1L, valoracion.getUsuarioId());
        assertEquals(PELICULA, valoracion.getPeliculaId());
        assertEquals(4, valoracion.getPuntuacion());
        assertEquals("Muy buena", valoracion.getComentario());
        assertEquals(1, repositorio.count());
    }

    @Test
    void aceptaLosLimitesDelRango() {
        servicioDe(1L).valorar(PELICULA, 1, null);
        servicioDe(2L).valorar(PELICULA, 5, null);

        assertEquals(2, repositorio.count());
    }

    @Test
    void rechazaPuntuacionesFueraDeRangoSinGuardar() {
        var servicio = servicioDe(1L);

        assertThrows(ValidacionException.class, () -> servicio.valorar(PELICULA, 0, null));
        assertThrows(ValidacionException.class, () -> servicio.valorar(PELICULA, 6, null));
        assertThrows(ValidacionException.class, () -> servicio.valorar(PELICULA, -3, null));
        assertEquals(0, repositorio.count());
    }

    @Test
    void valorarDeNuevoActualizaLaValoracionAnteriorSinCrearOtra() {
        var primera = servicioDe(1L).valorar(PELICULA, 2, "Floja");

        var segunda = servicioDe(1L).valorar(PELICULA, 5, "Mejor al verla otra vez");

        assertEquals(primera.getId(), segunda.getId());
        assertEquals(1, repositorio.count());
        assertEquals(5, repositorio.findAll().get(0).getPuntuacion());
        assertEquals("Mejor al verla otra vez", repositorio.findAll().get(0).getComentario());
    }

    @Test
    void cadaUsuarioTieneSuPropiaValoracion() {
        servicioDe(1L).valorar(PELICULA, 2, null);
        servicioDe(2L).valorar(PELICULA, 4, null);

        assertEquals(2, repositorio.count());
    }

    @Test
    void valorarUnaPeliculaInexistenteLanza404SinGuardar() {
        var servicio = servicioDe(1L);

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.valorar(99L, 3, null));
        assertEquals(0, repositorio.count());
    }

    @Test
    void laPrimeraValoracionFijaLaMediaDeLaPelicula() {
        servicioDe(1L).valorar(PELICULA, 4, null);

        assertEquals(4.0, peliculas.mediaDe(PELICULA));
    }

    @Test
    void recalculaLaMediaConTodasLasValoracionesDeLaPelicula() {
        servicioDe(1L).valorar(PELICULA, 5, null);
        servicioDe(2L).valorar(PELICULA, 2, null);
        servicioDe(3L).valorar(PELICULA, 2, null);

        assertEquals(3.0, peliculas.mediaDe(PELICULA));
    }

    @Test
    void cambiarLaValoracionRecalculaLaMedia() {
        servicioDe(1L).valorar(PELICULA, 5, null);
        servicioDe(2L).valorar(PELICULA, 1, null);

        servicioDe(2L).valorar(PELICULA, 3, null);

        assertEquals(4.0, peliculas.mediaDe(PELICULA));
    }

    @Test
    void laMediaSoloTieneEnCuentaLasValoracionesDeEsaPelicula() {
        peliculas.anadirPelicula(11L);
        servicioDe(1L).valorar(PELICULA, 5, null);
        servicioDe(1L).valorar(11L, 1, null);

        assertEquals(5.0, peliculas.mediaDe(PELICULA));
        assertEquals(1.0, peliculas.mediaDe(11L));
    }

    @Test
    void unaValoracionRechazadaNoTocaLaMedia() {
        servicioDe(1L).valorar(PELICULA, 4, null);

        assertThrows(ValidacionException.class, () -> servicioDe(2L).valorar(PELICULA, 9, null));

        assertEquals(4.0, peliculas.mediaDe(PELICULA));
    }

    @Test
    void laMediaConservaLosDecimales() {
        servicioDe(1L).valorar(PELICULA, 4, null);
        servicioDe(2L).valorar(PELICULA, 3, null);

        assertEquals(3.5, peliculas.mediaDe(PELICULA));
    }

    @Test
    void valorarDeNuevoSinComentarioBorraElComentarioAnterior() {
        servicioDe(1L).valorar(PELICULA, 4, "Muy buena");

        var actualizada = servicioDe(1L).valorar(PELICULA, 4, null);

        assertNull(actualizada.getComentario());
        assertNull(repositorio.findAll().get(0).getComentario());
    }

    @Test
    void valorarUnaPeliculaInexistenteNoGuardaMediaAlguna() {
        assertThrows(RecursoNoEncontradoException.class, () -> servicioDe(1L).valorar(99L, 3, null));

        assertNull(peliculas.mediaDe(99L));
    }
}

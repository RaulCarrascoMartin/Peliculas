package org.example.valoracion.internal.usecase;

import org.example.compartido.error.RecursoNoEncontradoException;
import org.example.compartido.seguridad.UsuarioActualFalso;
import org.example.pelicula.api.PeliculaResumen;
import org.example.valoracion.internal.domain.TipoMarca;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarcaPeliculaCasosDeUsoTest {

    private MarcaPeliculaRepositoryFalso repositorio;
    private PeliculaApiFalsa peliculas;

    @BeforeEach
    void preparar() {
        repositorio = new MarcaPeliculaRepositoryFalso();
        peliculas = new PeliculaApiFalsa();
        peliculas.anadirPelicula(10L);
        peliculas.anadirPelicula(11L);
    }

    /** Los tres casos de uso de marcas para un mismo usuario actual. */
    private record Marcas(MarcarPelicula marcarPelicula, DesmarcarPelicula desmarcarPelicula,
                          ListarPeliculasMarcadas listarMarcadas) {
        void marcar(Long peliculaId, TipoMarca tipo) {
            marcarPelicula.ejecutar(peliculaId, tipo);
        }

        void desmarcar(Long peliculaId, TipoMarca tipo) {
            desmarcarPelicula.ejecutar(peliculaId, tipo);
        }

        List<PeliculaResumen> peliculasMarcadas(TipoMarca tipo) {
            return listarMarcadas.ejecutar(tipo);
        }
    }

    private Marcas servicioDe(Long usuarioId) {
        var usuario = new UsuarioActualFalso(usuarioId, "USUARIO");
        return new Marcas(new MarcarPelicula(repositorio, peliculas, usuario),
                new DesmarcarPelicula(repositorio, usuario),
                new ListarPeliculasMarcadas(repositorio, peliculas, usuario));
    }

    private List<Long> idsMarcados(Long usuarioId, TipoMarca tipo) {
        return servicioDe(usuarioId).peliculasMarcadas(tipo).stream().map(PeliculaResumen::id).toList();
    }

    @Test
    void marcaUnaPeliculaComoFavoritaParaElUsuarioActual() {
        servicioDe(1L).marcar(10L, TipoMarca.FAVORITA);

        assertEquals(List.of(10L), idsMarcados(1L, TipoMarca.FAVORITA));
        assertTrue(servicioDe(1L).peliculasMarcadas(TipoMarca.VISTA).isEmpty());
    }

    @Test
    void marcarDosVecesLaMismaPeliculaNoLaDuplica() {
        servicioDe(1L).marcar(10L, TipoMarca.VISTA);
        servicioDe(1L).marcar(10L, TipoMarca.VISTA);

        assertEquals(1, repositorio.count());
    }

    @Test
    void unaPeliculaPuedeSerFavoritaYVista() {
        servicioDe(1L).marcar(10L, TipoMarca.FAVORITA);
        servicioDe(1L).marcar(10L, TipoMarca.VISTA);

        assertEquals(List.of(10L), idsMarcados(1L, TipoMarca.FAVORITA));
        assertEquals(List.of(10L), idsMarcados(1L, TipoMarca.VISTA));
    }

    @Test
    void marcarUnaPeliculaInexistenteLanza404SinGuardar() {
        var servicio = servicioDe(1L);

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.marcar(99L, TipoMarca.FAVORITA));
        assertEquals(0, repositorio.count());
    }

    @Test
    void desmarcaLaPeliculaSoloDeEsaLista() {
        servicioDe(1L).marcar(10L, TipoMarca.FAVORITA);
        servicioDe(1L).marcar(10L, TipoMarca.VISTA);

        servicioDe(1L).desmarcar(10L, TipoMarca.FAVORITA);

        assertTrue(servicioDe(1L).peliculasMarcadas(TipoMarca.FAVORITA).isEmpty());
        assertEquals(List.of(10L), idsMarcados(1L, TipoMarca.VISTA));
    }

    @Test
    void desmarcarUnaPeliculaNoMarcadaNoHaceNada() {
        servicioDe(1L).marcar(10L, TipoMarca.FAVORITA);

        servicioDe(1L).desmarcar(11L, TipoMarca.FAVORITA);
        servicioDe(1L).desmarcar(10L, TipoMarca.VISTA);

        assertEquals(1, repositorio.count());
    }

    @Test
    void cadaUsuarioSoloVeYModificaSusPropiasMarcas() {
        servicioDe(1L).marcar(10L, TipoMarca.FAVORITA);
        servicioDe(2L).marcar(11L, TipoMarca.FAVORITA);

        assertEquals(List.of(10L), idsMarcados(1L, TipoMarca.FAVORITA));
        assertEquals(List.of(11L), idsMarcados(2L, TipoMarca.FAVORITA));

        servicioDe(2L).desmarcar(10L, TipoMarca.FAVORITA);

        assertEquals(List.of(10L), idsMarcados(1L, TipoMarca.FAVORITA));
    }

    @Test
    void laListaIncluyeLosDatosDeLaPelicula() {
        servicioDe(1L).marcar(10L, TipoMarca.FAVORITA);

        var lista = servicioDe(1L).peliculasMarcadas(TipoMarca.FAVORITA);

        assertEquals(1, lista.size());
        assertEquals("Película 10", lista.get(0).titulo());
    }

    @Test
    void desmarcarUnaPeliculaInexistenteNoLanzaError() {
        servicioDe(1L).desmarcar(99L, TipoMarca.FAVORITA);

        assertEquals(0, repositorio.count());
    }

    @Test
    void sinMarcasLaListaEstaVacia() {
        assertTrue(servicioDe(1L).peliculasMarcadas(TipoMarca.VISTA).isEmpty());
    }
}

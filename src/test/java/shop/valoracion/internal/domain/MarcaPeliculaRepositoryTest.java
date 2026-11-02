package shop.valoracion.internal.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class MarcaPeliculaRepositoryTest {

    @Autowired
    private MarcaPeliculaRepository repositorio;

    private MarcaPelicula marca(Long usuarioId, Long peliculaId, TipoMarca tipo) {
        var marca = new MarcaPelicula();
        marca.setUsuarioId(usuarioId);
        marca.setPeliculaId(peliculaId);
        marca.setTipo(tipo);
        return marca;
    }

    @Test
    void guardaLaMarcaYAsignaId() {
        assertNotNull(repositorio.saveAndFlush(marca(1L, 10L, TipoMarca.FAVORITA)).getId());
    }

    @Test
    void buscaLaMarcaDeUnUsuarioPorPeliculaYTipo() {
        repositorio.saveAndFlush(marca(1L, 10L, TipoMarca.FAVORITA));

        var encontrada = repositorio.findByUsuarioIdAndPeliculaIdAndTipo(1L, 10L, TipoMarca.FAVORITA).orElseThrow();

        assertEquals(TipoMarca.FAVORITA, encontrada.getTipo());
        assertTrue(repositorio.findByUsuarioIdAndPeliculaIdAndTipo(1L, 10L, TipoMarca.VISTA).isEmpty());
        assertTrue(repositorio.findByUsuarioIdAndPeliculaIdAndTipo(2L, 10L, TipoMarca.FAVORITA).isEmpty());
    }

    @Test
    void unaPeliculaPuedeSerFavoritaYVistaALaVez() {
        repositorio.saveAndFlush(marca(1L, 10L, TipoMarca.FAVORITA));
        repositorio.saveAndFlush(marca(1L, 10L, TipoMarca.VISTA));

        assertEquals(2, repositorio.count());
    }

    @Test
    void listaLasMarcasDeUnUsuarioPorTipo() {
        repositorio.saveAndFlush(marca(1L, 10L, TipoMarca.FAVORITA));
        repositorio.saveAndFlush(marca(1L, 11L, TipoMarca.FAVORITA));
        repositorio.saveAndFlush(marca(1L, 12L, TipoMarca.VISTA));
        repositorio.saveAndFlush(marca(2L, 13L, TipoMarca.FAVORITA));

        assertEquals(2, repositorio.findByUsuarioIdAndTipo(1L, TipoMarca.FAVORITA).size());
        assertEquals(1, repositorio.findByUsuarioIdAndTipo(1L, TipoMarca.VISTA).size());
    }

    @Test
    void noPermiteMarcarDosVecesLaMismaPeliculaConElMismoTipo() {
        repositorio.saveAndFlush(marca(1L, 10L, TipoMarca.VISTA));

        assertThrows(DataIntegrityViolationException.class,
                () -> repositorio.saveAndFlush(marca(1L, 10L, TipoMarca.VISTA)));
    }

    @Test
    void usuarioPeliculaYTipoSonObligatorios() {
        assertThrows(DataIntegrityViolationException.class,
                () -> repositorio.saveAndFlush(marca(null, 10L, TipoMarca.VISTA)));
    }
}

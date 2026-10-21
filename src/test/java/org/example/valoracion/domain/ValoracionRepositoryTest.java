package org.example.valoracion.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class ValoracionRepositoryTest {

    @Autowired
    private ValoracionRepository repositorio;

    private Valoracion valoracion(Long usuarioId, Long peliculaId, int puntuacion, String comentario) {
        var valoracion = new Valoracion();
        valoracion.setUsuarioId(usuarioId);
        valoracion.setPeliculaId(peliculaId);
        valoracion.setPuntuacion(puntuacion);
        valoracion.setComentario(comentario);
        return valoracion;
    }

    @Test
    void guardaLaValoracionYAsignaId() {
        assertNotNull(repositorio.saveAndFlush(valoracion(1L, 10L, 4, "Muy buena")).getId());
    }

    @Test
    void buscaLaValoracionDeUnUsuarioParaUnaPelicula() {
        repositorio.saveAndFlush(valoracion(1L, 10L, 4, "Muy buena"));
        repositorio.saveAndFlush(valoracion(2L, 10L, 2, null));

        var encontrada = repositorio.findByUsuarioIdAndPeliculaId(1L, 10L).orElseThrow();

        assertEquals(4, encontrada.getPuntuacion());
        assertEquals("Muy buena", encontrada.getComentario());
        assertTrue(repositorio.findByUsuarioIdAndPeliculaId(1L, 11L).isEmpty());
    }

    @Test
    void listaLasValoracionesDeUnaPelicula() {
        repositorio.saveAndFlush(valoracion(1L, 10L, 4, null));
        repositorio.saveAndFlush(valoracion(2L, 10L, 2, null));
        repositorio.saveAndFlush(valoracion(1L, 11L, 5, null));

        assertEquals(2, repositorio.findByPeliculaId(10L).size());
    }

    @Test
    void elComentarioEsOpcional() {
        assertNull(repositorio.saveAndFlush(valoracion(1L, 10L, 3, null)).getComentario());
    }

    @Test
    void noPermiteDosValoracionesDelMismoUsuarioParaLaMismaPelicula() {
        repositorio.saveAndFlush(valoracion(1L, 10L, 4, null));

        assertThrows(DataIntegrityViolationException.class,
                () -> repositorio.saveAndFlush(valoracion(1L, 10L, 5, null)));
    }

    @Test
    void usuarioYPeliculaYPuntuacionSonObligatorios() {
        assertThrows(DataIntegrityViolationException.class,
                () -> repositorio.saveAndFlush(valoracion(null, 10L, 4, null)));
    }
}

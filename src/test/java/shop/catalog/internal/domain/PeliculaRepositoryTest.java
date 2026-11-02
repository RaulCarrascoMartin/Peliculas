package shop.catalog.internal.domain;

import jakarta.persistence.EntityManager;
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
class PeliculaRepositoryTest {

    @Autowired
    private PeliculaRepository repositorio;

    @Autowired
    private EntityManager entityManager;

    private Pelicula pelicula(String titulo) {
        var pelicula = new Pelicula();
        pelicula.setTitulo(titulo);
        pelicula.setAnio(1979);
        pelicula.setGenero("Ciencia ficción");
        pelicula.setDirector("Ridley Scott");
        return pelicula;
    }

    @Test
    void guardaLaPeliculaYAsignaId() {
        var guardada = repositorio.saveAndFlush(pelicula("Alien"));

        assertNotNull(guardada.getId());
    }

    @Test
    void recuperaTodosLosDatos() {
        var id = repositorio.saveAndFlush(pelicula("Alien")).getId();
        entityManager.clear();

        var recuperada = repositorio.findById(id).orElseThrow();

        assertEquals("Alien", recuperada.getTitulo());
        assertEquals(1979, recuperada.getAnio());
        assertEquals("Ciencia ficción", recuperada.getGenero());
        assertEquals("Ridley Scott", recuperada.getDirector());
    }

    @Test
    void sinValoracionesLaMediaEsNula() {
        var guardada = repositorio.saveAndFlush(pelicula("Alien"));

        assertNull(guardada.getValoracionMedia());
    }

    @Test
    void guardaLaValoracionMedia() {
        var pelicula = pelicula("Alien");
        pelicula.setValoracionMedia(4.5);
        var id = repositorio.saveAndFlush(pelicula).getId();
        entityManager.clear();

        assertEquals(4.5, repositorio.findById(id).orElseThrow().getValoracionMedia());
    }

    @Test
    void elTituloEsObligatorio() {
        assertThrows(DataIntegrityViolationException.class, () -> repositorio.saveAndFlush(pelicula(null)));
    }

    @Test
    void eliminaLaPelicula() {
        var id = repositorio.saveAndFlush(pelicula("Alien")).getId();

        repositorio.deleteById(id);

        assertTrue(repositorio.findById(id).isEmpty());
    }

    private void guardar(String titulo, int anio, String genero, String director) {
        var pelicula = pelicula(titulo);
        pelicula.setAnio(anio);
        pelicula.setGenero(genero);
        pelicula.setDirector(director);
        repositorio.saveAndFlush(pelicula);
    }

    @Test
    void buscaSinDistinguirMayusculasYCombinaFiltros() {
        guardar("Alien", 1979, "Ciencia ficción", "Ridley Scott");
        guardar("Aliens", 1986, "Ciencia ficción", "James Cameron");
        guardar("Gladiator", 2000, "Drama", "Ridley Scott");
        var pagina = org.springframework.data.domain.PageRequest.of(0, 10);

        assertEquals(2, repositorio.buscar("ALIEN", null, null, null, pagina).getTotalElements());
        assertEquals(1, repositorio.buscar("alien", null, 1986, null, pagina).getTotalElements());
        assertEquals(1, repositorio.buscar(null, "Drama", null, "Ridley Scott", pagina).getTotalElements());
        assertEquals(3, repositorio.buscar(null, null, null, null, pagina).getTotalElements());
        assertTrue(repositorio.buscar("alien", "Drama", null, null, pagina).isEmpty());
    }
}

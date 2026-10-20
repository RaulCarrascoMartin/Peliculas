package org.example.pelicula.domain;

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
}

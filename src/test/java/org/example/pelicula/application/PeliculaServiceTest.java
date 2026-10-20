package org.example.pelicula.application;

import org.example.compartido.RecursoNoEncontradoException;
import org.example.pelicula.domain.Pelicula;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeliculaServiceTest {

    private PeliculaRepositoryFalso repositorio;
    private PeliculaService servicio;

    @BeforeEach
    void preparar() {
        repositorio = new PeliculaRepositoryFalso();
        servicio = new PeliculaService(repositorio);
    }

    private static Pelicula pelicula(String titulo, int anio, String genero, String director) {
        var pelicula = new Pelicula();
        pelicula.setTitulo(titulo);
        pelicula.setAnio(anio);
        pelicula.setGenero(genero);
        pelicula.setDirector(director);
        return pelicula;
    }

    @Test
    void creaLaPeliculaYDevuelveElIdAsignado() {
        var creada = servicio.crear(pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott"));

        assertNotNull(creada.getId());
        assertEquals("Alien", repositorio.findById(creada.getId()).orElseThrow().getTitulo());
    }

    @Test
    void devuelveElDetalleDeUnaPeliculaExistente() {
        var id = servicio.crear(pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott")).getId();

        assertEquals("Alien", servicio.detalle(id).getTitulo());
    }

    @Test
    void detalleDeUnaPeliculaInexistenteLanza404() {
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.detalle(99L));
    }

    @Test
    void editaLosDatosYConservaLaValoracionMedia() {
        var original = pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott");
        original.setValoracionMedia(4.5);
        var id = servicio.crear(original).getId();

        var editada = servicio.editar(id, pelicula("Aliens", 1986, "Acción", "James Cameron"));

        assertEquals(id, editada.getId());
        assertEquals("Aliens", editada.getTitulo());
        assertEquals(1986, editada.getAnio());
        assertEquals("Acción", editada.getGenero());
        assertEquals("James Cameron", editada.getDirector());
        assertEquals(4.5, editada.getValoracionMedia());
        assertEquals(1, repositorio.count());
    }

    @Test
    void editarUnaPeliculaInexistenteLanza404() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.editar(99L, pelicula("Aliens", 1986, "Acción", "James Cameron")));
        assertEquals(0, repositorio.count());
    }

    @Test
    void eliminaLaPelicula() {
        var id = servicio.crear(pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott")).getId();

        servicio.eliminar(id);

        assertTrue(repositorio.findById(id).isEmpty());
    }

    @Test
    void eliminarUnaPeliculaInexistenteLanza404() {
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar(99L));
    }

    @Test
    void existeDevuelveTrueSoloParaPeliculasCreadas() {
        var id = servicio.crear(pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott")).getId();

        assertTrue(servicio.existe(id));
        assertFalse(servicio.existe(99L));
    }

    @Test
    void actualizaLaValoracionMediaSinTocarElResto() {
        var id = servicio.crear(pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott")).getId();

        servicio.actualizarValoracionMedia(id, 3.5);

        var guardada = repositorio.findById(id).orElseThrow();
        assertEquals(3.5, guardada.getValoracionMedia());
        assertEquals("Alien", guardada.getTitulo());
    }

    @Test
    void actualizarLaValoracionMediaDeUnaPeliculaInexistenteLanza404() {
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.actualizarValoracionMedia(99L, 3.5));
    }

    @Test
    void listaLasPeliculasPaginadas() {
        for (int i = 1; i <= 5; i++) {
            servicio.crear(pelicula("Película " + i, 2000 + i, "Drama", "Director"));
        }

        var pagina = servicio.listar(PageRequest.of(1, 2));

        assertEquals(5, pagina.getTotalElements());
        assertEquals(3, pagina.getTotalPages());
        assertEquals(2, pagina.getContent().size());
        assertEquals("Película 3", pagina.getContent().get(0).getTitulo());
    }

    @Test
    void listarSinPeliculasDevuelveUnaPaginaVacia() {
        assertTrue(servicio.listar(PageRequest.of(0, 10)).isEmpty());
    }
}

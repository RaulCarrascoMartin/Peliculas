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

        var pagina = servicio.buscar(null, null, null, null, PageRequest.of(1, 2));

        assertEquals(5, pagina.getTotalElements());
        assertEquals(3, pagina.getTotalPages());
        assertEquals(2, pagina.getContent().size());
        assertEquals("Película 3", pagina.getContent().get(0).getTitulo());
    }

    @Test
    void listarSinPeliculasDevuelveUnaPaginaVacia() {
        assertTrue(servicio.buscar(null, null, null, null, PageRequest.of(0, 10)).isEmpty());
    }

    private void crearCatalogo() {
        servicio.crear(pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott"));
        servicio.crear(pelicula("Aliens", 1986, "Ciencia ficción", "James Cameron"));
        servicio.crear(pelicula("Gladiator", 2000, "Drama", "Ridley Scott"));
        servicio.crear(pelicula("Titanic", 1997, "Drama", "James Cameron"));
    }

    private java.util.List<String> titulos(String titulo, String genero, Integer anio, String director) {
        return servicio.buscar(titulo, genero, anio, director, PageRequest.of(0, 10)).getContent().stream()
                .map(Pelicula::getTitulo).toList();
    }

    @Test
    void buscaPorTituloSinDistinguirMayusculasYPorFragmento() {
        crearCatalogo();

        assertEquals(java.util.List.of("Alien", "Aliens"), titulos("ALIEN", null, null, null));
        assertEquals(java.util.List.of("Gladiator"), titulos("diat", null, null, null));
    }

    @Test
    void filtraPorGeneroAnioYDirector() {
        crearCatalogo();

        assertEquals(java.util.List.of("Gladiator", "Titanic"), titulos(null, "Drama", null, null));
        assertEquals(java.util.List.of("Titanic"), titulos(null, null, 1997, null));
        assertEquals(java.util.List.of("Alien", "Gladiator"), titulos(null, null, null, "Ridley Scott"));
    }

    @Test
    void combinaTodosLosFiltrosIndicados() {
        crearCatalogo();

        assertEquals(java.util.List.of("Gladiator"), titulos(null, "Drama", null, "Ridley Scott"));
        assertEquals(java.util.List.of("Aliens"), titulos("alien", "Ciencia ficción", 1986, "James Cameron"));
        assertTrue(titulos("alien", "Drama", null, null).isEmpty());
    }

    @Test
    void ignoraLosFiltrosEnBlanco() {
        crearCatalogo();

        assertEquals(4, titulos("  ", "", null, " ").size());
    }

    @Test
    void sinCoincidenciasDevuelveListaVacia() {
        crearCatalogo();

        assertTrue(titulos("zzz", null, null, null).isEmpty());
        assertTrue(titulos(null, null, 1800, null).isEmpty());
    }

    @Test
    void resumenesDevuelveLasPeliculasPedidasOrdenadasPorIdIgnorandoLasInexistentes() {
        servicio.crear(pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott"));
        servicio.crear(pelicula("Aliens", 1986, "Ciencia ficción", "James Cameron"));
        servicio.crear(pelicula("Gladiator", 2000, "Drama", "Ridley Scott"));
        servicio.actualizarValoracionMedia(3L, 4.5);

        var resumenes = servicio.resumenes(java.util.List.of(3L, 1L, 99L));

        assertEquals(2, resumenes.size());
        assertEquals(1L, resumenes.get(0).id());
        assertEquals("Alien", resumenes.get(0).titulo());
        assertEquals(3L, resumenes.get(1).id());
        assertEquals("Gladiator", resumenes.get(1).titulo());
        assertEquals(2000, resumenes.get(1).anio());
        assertEquals("Drama", resumenes.get(1).genero());
        assertEquals("Ridley Scott", resumenes.get(1).director());
        assertEquals(4.5, resumenes.get(1).valoracionMedia());
    }

    @Test
    void resumenesSinIdsDevuelveListaVacia() {
        assertTrue(servicio.resumenes(java.util.List.of()).isEmpty());
    }
}

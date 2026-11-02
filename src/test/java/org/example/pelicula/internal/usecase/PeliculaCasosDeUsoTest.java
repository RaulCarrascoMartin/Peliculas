package org.example.pelicula.internal.usecase;

import org.example.compartido.RecursoNoEncontradoException;
import org.example.pelicula.api.PeliculaEliminada;
import org.example.pelicula.internal.PeliculaApiImpl;
import org.example.pelicula.internal.domain.Pelicula;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeliculaCasosDeUsoTest {

    private PeliculaRepositoryFalso repositorio;
    private CrearPelicula crear;
    private ObtenerPelicula obtener;
    private BuscarPeliculas buscar;
    private EditarPelicula editar;
    private EliminarPelicula eliminar;
    private PeliculaApiImpl api;
    private final java.util.List<Object> eventos = new java.util.ArrayList<>();

    @BeforeEach
    void preparar() {
        repositorio = new PeliculaRepositoryFalso();
        crear = new CrearPelicula(repositorio);
        obtener = new ObtenerPelicula(repositorio);
        buscar = new BuscarPeliculas(repositorio);
        editar = new EditarPelicula(repositorio, obtener);
        eliminar = new EliminarPelicula(repositorio, eventos::add);
        api = new PeliculaApiImpl(repositorio, obtener);
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
        var creada = crear.ejecutar(pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott"));

        assertNotNull(creada.getId());
        assertEquals("Alien", repositorio.findById(creada.getId()).orElseThrow().getTitulo());
    }

    @Test
    void devuelveElDetalleDeUnaPeliculaExistente() {
        var id = crear.ejecutar(pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott")).getId();

        assertEquals("Alien", obtener.ejecutar(id).getTitulo());
    }

    @Test
    void detalleDeUnaPeliculaInexistenteLanza404() {
        assertThrows(RecursoNoEncontradoException.class, () -> obtener.ejecutar(99L));
    }

    @Test
    void editaLosDatosYConservaLaValoracionMedia() {
        var original = pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott");
        original.setValoracionMedia(4.5);
        var id = crear.ejecutar(original).getId();

        var editada = editar.ejecutar(id, pelicula("Aliens", 1986, "Acción", "James Cameron"));

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
                () -> editar.ejecutar(99L, pelicula("Aliens", 1986, "Acción", "James Cameron")));
        assertEquals(0, repositorio.count());
    }

    @Test
    void eliminaLaPelicula() {
        var id = crear.ejecutar(pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott")).getId();

        eliminar.ejecutar(id);

        assertTrue(repositorio.findById(id).isEmpty());
    }

    @Test
    void eliminarUnaPeliculaInexistenteLanza404() {
        assertThrows(RecursoNoEncontradoException.class, () -> eliminar.ejecutar(99L));
    }

    @Test
    void existeDevuelveTrueSoloParaPeliculasCreadas() {
        var id = crear.ejecutar(pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott")).getId();

        assertTrue(api.existe(id));
        assertFalse(api.existe(99L));
    }

    @Test
    void actualizaLaValoracionMediaSinTocarElResto() {
        var id = crear.ejecutar(pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott")).getId();

        api.actualizarValoracionMedia(id, 3.5);

        var guardada = repositorio.findById(id).orElseThrow();
        assertEquals(3.5, guardada.getValoracionMedia());
        assertEquals("Alien", guardada.getTitulo());
    }

    @Test
    void actualizarLaValoracionMediaDeUnaPeliculaInexistenteLanza404() {
        assertThrows(RecursoNoEncontradoException.class, () -> api.actualizarValoracionMedia(99L, 3.5));
    }

    @Test
    void listaLasPeliculasPaginadas() {
        for (int i = 1; i <= 5; i++) {
            crear.ejecutar(pelicula("Película " + i, 2000 + i, "Drama", "Director"));
        }

        var pagina = buscar.ejecutar(null, null, null, null, PageRequest.of(1, 2));

        assertEquals(5, pagina.getTotalElements());
        assertEquals(3, pagina.getTotalPages());
        assertEquals(2, pagina.getContent().size());
        assertEquals("Película 3", pagina.getContent().get(0).getTitulo());
    }

    @Test
    void listarSinPeliculasDevuelveUnaPaginaVacia() {
        assertTrue(buscar.ejecutar(null, null, null, null, PageRequest.of(0, 10)).isEmpty());
    }

    private void crearCatalogo() {
        crear.ejecutar(pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott"));
        crear.ejecutar(pelicula("Aliens", 1986, "Ciencia ficción", "James Cameron"));
        crear.ejecutar(pelicula("Gladiator", 2000, "Drama", "Ridley Scott"));
        crear.ejecutar(pelicula("Titanic", 1997, "Drama", "James Cameron"));
    }

    private java.util.List<String> titulos(String titulo, String genero, Integer anio, String director) {
        return buscar.ejecutar(titulo, genero, anio, director, PageRequest.of(0, 10)).getContent().stream()
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
        var alien = pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott");
        alien.setPrecio(new BigDecimal("12.99"));
        var gladiator = pelicula("Gladiator", 2000, "Drama", "Ridley Scott");
        // preexistente sin precio
        crear.ejecutar(alien);
        var gladiatorId = crear.ejecutar(gladiator).getId();
        api.actualizarValoracionMedia(gladiatorId, 4.5);

        var resumenes = api.resumenes(java.util.List.of(gladiatorId, alien.getId().longValue(), 99L));

        assertEquals(2, resumenes.size());
        assertEquals("Alien", resumenes.get(0).titulo());
        assertEquals(new BigDecimal("12.99"), resumenes.get(0).precio());
        assertEquals("Gladiator", resumenes.get(1).titulo());
        assertEquals(2000, resumenes.get(1).anio());
        assertEquals("Drama", resumenes.get(1).genero());
        assertEquals("Ridley Scott", resumenes.get(1).director());
        assertNull(resumenes.get(1).precio());
        assertEquals(4.5, resumenes.get(1).valoracionMedia());
    }

    @Test
    void guardarElPrecioAlCrear() {
        var alien = pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott");
        alien.setPrecio(new BigDecimal("12.99"));

        var id = crear.ejecutar(alien).getId();

        assertEquals(new BigDecimal("12.99"), obtener.ejecutar(id).getPrecio());
    }

    @Test
    void editarActualizaElPrecioYConservaElResto() {
        var alien = pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott");
        alien.setPrecio(new BigDecimal("12.99"));
        var id = crear.ejecutar(alien).getId();

        var aliens = pelicula("Alien (remasterizada)", 1979, "Ciencia ficción", "Ridley Scott");
        aliens.setPrecio(new BigDecimal("19.99"));
        var editada = editar.ejecutar(id, aliens);

        assertEquals(new BigDecimal("19.99"), editada.getPrecio());
        assertEquals(new BigDecimal("19.99"), repositorio.findById(id).orElseThrow().getPrecio());
    }

    @Test
    void resumenesSinIdsDevuelveListaVacia() {
        assertTrue(api.resumenes(java.util.List.of()).isEmpty());
    }

    @Test
    void eliminarPublicaElEventoPeliculaEliminada() {
        var id = crear.ejecutar(pelicula("Alien", 1979, "Ciencia ficción", "Ridley Scott")).getId();

        eliminar.ejecutar(id);

        assertEquals(java.util.List.of(new PeliculaEliminada(id)), eventos);
    }

    @Test
    void eliminarUnaPeliculaInexistenteNoPublicaNingunEvento() {
        assertThrows(RecursoNoEncontradoException.class, () -> eliminar.ejecutar(99L));

        assertTrue(eventos.isEmpty());
    }
}

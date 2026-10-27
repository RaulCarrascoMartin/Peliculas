package org.example.carrito.application;

import org.example.carrito.domain.Carrito;
import org.example.carrito.domain.CarritoRepository;
import org.example.carrito.domain.LineaCarrito;
import org.example.carrito.domain.LineaCarritoRepository;
import org.example.compartido.ConflictoException;
import org.example.compartido.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarritoServiceTest {

    private static final BigDecimal PRECIO = new BigDecimal("12.99");

    private CarritoRepositoryFalso carritos;
    private LineaCarritoRepositoryFalso lineas;
    private PeliculaApiFalsa peliculas;
    private CarritoService servicio;

    @BeforeEach
    void preparar() {
        carritos = new CarritoRepositoryFalso();
        lineas = new LineaCarritoRepositoryFalso();
        peliculas = new PeliculaApiFalsa();
        servicio = new CarritoService(carritos, lineas, peliculas);
    }

    private void crearPelicula(Long id, BigDecimal precio) {
        peliculas.crearPelicula(id, precio);
    }

    private void lineaDirecta(Long carritoId, Long peliculaId) {
        var linea = new LineaCarrito();
        linea.setCarritoId(carritoId);
        linea.setPeliculaId(peliculaId);
        lineas.save(linea);
    }

    @Test
    void verSinCarritoDevuelveUnCarritoVacioConTotalCero() {
        CarritoVista vista = servicio.ver(1L);

        assertTrue(vista.lineas().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(vista.total()));
    }

    @Test
    void verMuestraElPrecioVivoDeCadaPeliculaYSumaElTotal() {
        crearPelicula(10L, PRECIO);
        crearPelicula(11L, new BigDecimal("5.00"));
        servicio.anadir(1L, 10L);
        servicio.anadir(1L, 11L);
        peliculas.cambiarPrecio(10L, new BigDecimal("15.50"));

        CarritoVista vista = servicio.ver(1L);

        assertEquals(List.of(10L, 11L), vista.lineas().stream().map(LineaDeCarrito::peliculaId).toList());
        assertEquals(0, new BigDecimal("15.50").compareTo(vista.lineas().get(0).precio()));
        assertEquals(0, new BigDecimal("5.00").compareTo(vista.lineas().get(1).precio()));
        assertEquals(0, new BigDecimal("20.50").compareTo(vista.total()));
    }

    @Test
    void verDevuelveSoloLasLineasDelUsuario() {
        crearPelicula(10L, PRECIO);
        crearPelicula(11L, PRECIO);
        servicio.anadir(1L, 10L);
        servicio.anadir(2L, 11L);

        CarritoVista vista = servicio.ver(1L);

        assertEquals(List.of("Película 10"), vista.lineas().stream().map(LineaDeCarrito::titulo).toList());
        assertEquals(1, vista.lineas().size());
    }

    @Test
    void anadirLaPrimeraPeliculaCreaElCarritoYDevuelveLaLineaConElPrecioActual() {
        crearPelicula(10L, PRECIO);
        assertTrue(carritos.findByUsuarioId(1L).isEmpty());

        LineaDeCarrito linea = servicio.anadir(1L, 10L);

        assertTrue(carritos.findByUsuarioId(1L).isPresent());
        assertEquals(10L, linea.peliculaId());
        assertEquals("Película 10", linea.titulo());
        assertEquals(0, PRECIO.compareTo(linea.precio()));
        assertEquals(1, lineas.count());
    }

    @Test
    void anteUnaCarreraPorLaCreacionSeReleeElCarritoDelOtroHilo() {
        crearPelicula(10L, PRECIO);
        var otroCarrito = new Carrito();
        otroCarrito.setUsuarioId(1L);
        carritos.save(otroCarrito);
        carritos.ocultarElCarritoEnProximaConsulta(1L);

        LineaDeCarrito linea = servicio.anadir(1L, 10L);

        assertEquals(10L, linea.peliculaId());
        assertEquals(0, PRECIO.compareTo(linea.precio()));
        assertEquals(1, carritos.count());
        assertEquals(1, lineas.count());
        assertEquals(1, servicio.ver(1L).lineas().size());
    }

    @Test
    void anadirUnaPeliculaYaEnElCarritoDaConflicto() {
        crearPelicula(10L, PRECIO);
        servicio.anadir(1L, 10L);

        assertThrows(ConflictoException.class, () -> servicio.anadir(1L, 10L));

        assertEquals(1, lineas.count());
    }

    @Test
    void anadirUnaPeliculaInexistenteDaRecursoNoEncontradoYNoCreaCarrito() {
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.anadir(1L, 99L));

        assertEquals(0, carritos.count());
        assertEquals(0, lineas.count());
    }

    @Test
    void anadirUnaPeliculaSinPrecioDaConflicto() {
        crearPelicula(10L, null);

        assertThrows(ConflictoException.class, () -> servicio.anadir(1L, 10L));

        assertEquals(0, lineas.count());
    }

    @Test
    void quitarUnaPeliculaQueNoEstaEnElCarritoDaRecursoNoEncontrado() {
        crearPelicula(10L, PRECIO);

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.quitar(1L, 10L));

        servicio.anadir(1L, 10L);
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.quitar(1L, 99L));
        assertEquals(1, lineas.count());
    }

    @Test
    void quitarEliminaLaLineaYDejaElCarritoVacioPeroExistente() {
        crearPelicula(10L, PRECIO);
        servicio.anadir(1L, 10L);

        servicio.quitar(1L, 10L);

        assertEquals(0, lineas.count());
        assertTrue(carritos.findByUsuarioId(1L).isPresent());
        assertTrue(servicio.ver(1L).lineas().isEmpty());
    }

    @Test
    void vaciarDejaElCarritoSinLineasYEsIdempotente() {
        crearPelicula(10L, PRECIO);
        crearPelicula(11L, PRECIO);
        servicio.anadir(1L, 10L);
        servicio.anadir(1L, 11L);

        servicio.vaciar(1L);
        assertEquals(0, lineas.count());

        servicio.vaciar(1L);
        assertTrue(carritos.findByUsuarioId(1L).isPresent());

        servicio.vaciar(2L); // sin carrito tampoco falla
        assertEquals(0, lineas.count());
    }

    @Test
    void laLineaHuerfanaSeOmiteDelListadoYDelTotal() {
        crearPelicula(10L, PRECIO);
        servicio.anadir(1L, 10L);
        Long carritoId = carritos.findByUsuarioId(1L).orElseThrow().getId();
        lineaDirecta(carritoId, 99L); // huérfana: la película 99 ya no existe

        CarritoVista vista = servicio.ver(1L);

        assertEquals(List.of(10L), vista.lineas().stream().map(LineaDeCarrito::peliculaId).toList());
        assertEquals(0, PRECIO.compareTo(vista.total()));
    }

    @Test
    void peliculasEnCarritoDevuelveLosIdsDeLasLineasPropias() {
        crearPelicula(10L, PRECIO);
        crearPelicula(11L, PRECIO);
        servicio.anadir(2L, 11L);
        servicio.anadir(1L, 10L);

        assertEquals(List.of(10L), servicio.peliculasEnCarrito(1L));
        assertTrue(servicio.peliculasEnCarrito(3L).isEmpty());
    }
}

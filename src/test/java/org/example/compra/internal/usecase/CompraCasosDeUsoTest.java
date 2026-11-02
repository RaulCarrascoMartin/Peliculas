package org.example.compra.internal.usecase;

import org.example.compartido.ConflictoException;
import org.example.compartido.RecursoNoEncontradoException;
import org.example.compra.internal.domain.Compra;
import org.example.compra.internal.domain.EstadoCompra;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompraCasosDeUsoTest {

    private CompraRepositoryFalso compras;
    private LineaCompraRepositoryFalso lineas;
    private CarritoApiFalsa carritos;
    private PeliculaApiFalsa peliculas;
    private FinalizarCompra finalizar;
    private ListarCompras listar;
    private ObtenerCompra obtener;

    @BeforeEach
    void preparar() {
        compras = new CompraRepositoryFalso();
        lineas = new LineaCompraRepositoryFalso();
        carritos = new CarritoApiFalsa();
        peliculas = new PeliculaApiFalsa();
        finalizar = new FinalizarCompra(compras, lineas, carritos, peliculas);
        listar = new ListarCompras(compras, lineas);
        obtener = new ObtenerCompra(compras, lineas);
    }

    private void carritoConDosPeliculas(Long usuarioId) {
        peliculas.crearPelicula(10L, "Matrix", new BigDecimal("7.99"));
        peliculas.crearPelicula(11L, "Origen", new BigDecimal("9.49"));
        carritos.anadir(usuarioId, 10L);
        carritos.anadir(usuarioId, 11L);
    }

    @Test
    void finalizarCreaLaCompraConSnapshotDeTituloPrecioYTotalYVaciaElCarrito() {
        carritoConDosPeliculas(1L);

        CompraVista vista = finalizar.ejecutar(1L);

        assertNotNull(vista.compra().getId());
        assertEquals(1L, vista.compra().getUsuarioId());
        assertEquals(EstadoCompra.COMPRADA, vista.compra().getEstado());
        assertNotNull(vista.compra().getFecha());
        assertEquals(new BigDecimal("17.48"), vista.compra().getTotal());
        assertEquals(List.of("Matrix", "Origen"), vista.lineas().stream().map(l -> l.getTitulo()).toList());
        assertEquals(List.of(new BigDecimal("7.99"), new BigDecimal("9.49")),
                vista.lineas().stream().map(l -> l.getPrecioUnitario()).toList());
        assertEquals(vista.compra().getId(), vista.lineas().get(0).getCompraId());
        assertEquals(2, lineas.count());
        assertTrue(carritos.peliculasEnCarrito(1L).isEmpty());
    }

    @Test
    void finalizarUsaElPrecioVigenteSiCambioDesdeQueSeAnadioAlCarrito() {
        carritoConDosPeliculas(1L);
        peliculas.cambiarPrecio(10L, new BigDecimal("10.00"));

        CompraVista vista = finalizar.ejecutar(1L);

        assertEquals(new BigDecimal("10.00"), vista.lineas().get(0).getPrecioUnitario());
        assertEquals(new BigDecimal("19.49"), vista.compra().getTotal());
    }

    @Test
    void laCompraConservaTituloYPrecioAunqueLaPeliculaSeEditeOSeElimine() {
        carritoConDosPeliculas(1L);
        Long compraId = finalizar.ejecutar(1L).compra().getId();

        peliculas.cambiarPrecio(10L, new BigDecimal("99.00"));
        peliculas.cambiarTitulo(10L, "Matrix Reloaded");
        peliculas.eliminar(11L);

        CompraVista detalle = obtener.ejecutar(1L, compraId);
        assertEquals(new BigDecimal("17.48"), detalle.compra().getTotal());
        assertEquals(List.of("Matrix", "Origen"), detalle.lineas().stream().map(l -> l.getTitulo()).toList());
        assertEquals(new BigDecimal("7.99"), detalle.lineas().get(0).getPrecioUnitario());
    }

    @Test
    void finalizarConCarritoVacioOInexistenteEsConflicto() {
        assertThrows(ConflictoException.class, () -> finalizar.ejecutar(1L));
        assertEquals(0, compras.count());
    }

    @Test
    void finalizarConUnaPeliculaDesaparecidaEsConflictoYDejaElCarritoIntacto() {
        carritoConDosPeliculas(1L);
        peliculas.eliminar(11L);

        assertThrows(ConflictoException.class, () -> finalizar.ejecutar(1L));

        assertEquals(List.of(10L, 11L), carritos.peliculasEnCarrito(1L));
        assertEquals(0, compras.count());
        assertEquals(0, lineas.count());
    }

    @Test
    void finalizarConUnaPeliculaSinPrecioEsConflictoYDejaElCarritoIntacto() {
        carritoConDosPeliculas(1L);
        peliculas.cambiarPrecio(10L, null);

        assertThrows(ConflictoException.class, () -> finalizar.ejecutar(1L));

        assertEquals(List.of(10L, 11L), carritos.peliculasEnCarrito(1L));
        assertEquals(0, compras.count());
    }

    @Test
    void elBloqueoOptimistaAlVaciarSeConvierteEnConflicto() {
        carritoConDosPeliculas(1L);
        carritos.fallarAlVaciarPorBloqueoOptimista();

        assertThrows(ConflictoException.class, () -> finalizar.ejecutar(1L));
    }

    @Test
    void listarDevuelveSoloLasComprasPropiasDeLaMasRecienteALaMasAntigua() {
        Long antigua = compraDirecta(1L, LocalDateTime.of(2026, 11, 1, 9, 0)).getId();
        Long reciente = compraDirecta(1L, LocalDateTime.of(2026, 11, 3, 9, 0)).getId();
        compraDirecta(2L, LocalDateTime.of(2026, 11, 4, 9, 0));

        List<Long> ids = listar.ejecutar(1L).stream().map(v -> v.compra().getId()).toList();

        assertEquals(List.of(reciente, antigua), ids);
    }

    @Test
    void detalleDevuelveLaCompraPropiaConSusLineas() {
        carritoConDosPeliculas(1L);
        Long compraId = finalizar.ejecutar(1L).compra().getId();

        CompraVista detalle = obtener.ejecutar(1L, compraId);

        assertEquals(compraId, detalle.compra().getId());
        assertEquals(2, detalle.lineas().size());
    }

    @Test
    void detalleDeUnaCompraAjenaOInexistenteEsNoEncontrado() {
        carritoConDosPeliculas(2L);
        Long ajena = finalizar.ejecutar(2L).compra().getId();

        assertThrows(RecursoNoEncontradoException.class, () -> obtener.ejecutar(1L, ajena));
        assertThrows(RecursoNoEncontradoException.class, () -> obtener.ejecutar(1L, 999L));
    }

    private Compra compraDirecta(Long usuarioId, LocalDateTime fecha) {
        var compra = new Compra();
        compra.setUsuarioId(usuarioId);
        compra.setFecha(fecha);
        compra.setTotal(BigDecimal.ZERO);
        compra.setEstado(EstadoCompra.COMPRADA);
        return compras.save(compra);
    }
}

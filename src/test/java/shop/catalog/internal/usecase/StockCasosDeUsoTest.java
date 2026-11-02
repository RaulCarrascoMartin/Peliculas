package shop.catalog.internal.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.catalog.api.InsufficientStockException;
import shop.catalog.api.ProductNotPurchasableException;
import shop.catalog.api.ReservedLine;
import shop.catalog.api.StockLine;
import shop.catalog.api.UnknownProductException;
import shop.catalog.internal.CatalogApiImpl;
import shop.catalog.internal.domain.Pelicula;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Módulo catalog aislado: la reserva y la liberación mantienen el stock coherente. */
class StockCasosDeUsoTest {

    private PeliculaRepositoryFalso repositorio;
    private CatalogApiImpl api;

    @BeforeEach
    void preparar() {
        repositorio = new PeliculaRepositoryFalso();
        api = new CatalogApiImpl(repositorio, new ReservarStock(repositorio), new LiberarStock(repositorio));
    }

    private Long producto(String titulo, String precio, int stock) {
        var p = new Pelicula();
        p.setTitulo(titulo);
        p.setPrecio(precio == null ? null : new BigDecimal(precio));
        p.setStock(stock);
        return repositorio.save(p).getId();
    }

    private int stockDe(Long id) {
        return repositorio.findById(id).orElseThrow().getStock();
    }

    @Test
    void reservarDescuentaElStockYCapturaTituloYPrecio() {
        Long alien = producto("Alien", "12.99", 5);
        Long matrix = producto("Matrix", "7.50", 2);

        List<ReservedLine> reservadas = api.reserve(List.of(new StockLine(alien, 3), new StockLine(matrix, 2)));

        assertEquals(2, stockDe(alien));
        assertEquals(0, stockDe(matrix));
        assertEquals(new ReservedLine(alien, "Alien", new BigDecimal("12.99"), 3), reservadas.get(0));
        assertEquals(new ReservedLine(matrix, "Matrix", new BigDecimal("7.50"), 2), reservadas.get(1));
    }

    @Test
    void siUnaLineaFallaElStockQuedaComoEstaba() {
        Long alien = producto("Alien", "12.99", 5);
        Long matrix = producto("Matrix", "7.50", 1);

        assertThrows(InsufficientStockException.class,
                () -> api.reserve(List.of(new StockLine(alien, 2), new StockLine(matrix, 2))));

        assertEquals(5, stockDe(alien));
        assertEquals(1, stockDe(matrix));
    }

    @Test
    void unProductoDesconocidoFallaYNoTocaElResto() {
        Long alien = producto("Alien", "12.99", 5);

        assertThrows(UnknownProductException.class,
                () -> api.reserve(List.of(new StockLine(alien, 1), new StockLine(999L, 1))));

        assertEquals(5, stockDe(alien));
    }

    @Test
    void unProductoSinPrecioNoSePuedeReservar() {
        Long sinPrecio = producto("Sin precio", null, 5);

        assertThrows(ProductNotPurchasableException.class, () -> api.reserve(List.of(new StockLine(sinPrecio, 1))));

        assertEquals(5, stockDe(sinPrecio));
    }

    @Test
    void laMismaLineaRepetidaSeSumaAntesDeComprobarElStock() {
        Long alien = producto("Alien", "12.99", 3);

        assertThrows(InsufficientStockException.class,
                () -> api.reserve(List.of(new StockLine(alien, 2), new StockLine(alien, 2))));

        assertEquals(3, stockDe(alien));
    }

    @Test
    void liberarDevuelveLasUnidadesYReservarDespuesVuelveAFuncionar() {
        Long alien = producto("Alien", "12.99", 2);
        api.reserve(List.of(new StockLine(alien, 2)));
        assertEquals(0, stockDe(alien));

        api.release(List.of(new StockLine(alien, 2)));

        assertEquals(2, stockDe(alien));
        api.reserve(List.of(new StockLine(alien, 2)));
        assertEquals(0, stockDe(alien));
    }

    @Test
    void liberarUnProductoInexistenteNoFalla() {
        api.release(List.of(new StockLine(999L, 1)));
    }
}

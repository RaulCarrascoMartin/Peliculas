package shop.catalog.internal.application.usecase;

import shop.catalog.internal.application.facade.CatalogFacadeImpl;
import shop.catalog.internal.domain.fake.MovieRepositoryFake;

import shop.catalog.internal.domain.service.MovieServiceImpl;
import shop.catalog.internal.domain.service.StockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.catalog.api.facade.exception.InsufficientStockException;
import shop.catalog.api.facade.exception.ProductNotPurchasableException;
import shop.catalog.api.facade.dto.ReservedLineDto;
import shop.catalog.api.facade.dto.StockLineDto;
import shop.catalog.api.facade.exception.UnknownProductException;
import shop.catalog.internal.application.facade.CatalogFacadeImpl;
import shop.catalog.internal.domain.model.Movie;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Isolated catalog module: reserve and release keep the stock consistent. */
class StockUseCasesTest {

    private MovieRepositoryFake repository;
    private CatalogFacadeImpl api;

    @BeforeEach
    void setUp() {
        repository = new MovieRepositoryFake();
        var stock = new StockServiceImpl(repository);
        api = new CatalogFacadeImpl(new MovieServiceImpl(repository), new ReserveStock(stock), new ReleaseStock(stock));
    }

    private Long product(String title, String price, int stock) {
        var p = new Movie();
        p.setTitle(title);
        p.setPrice(price == null ? null : new BigDecimal(price));
        p.setStock(stock);
        return repository.save(p).getId();
    }

    private int stockOf(Long id) {
        return repository.findById(id).orElseThrow().getStock();
    }

    @Test
    void reserveDeductsStockAndCapturesTitleAndPrice() {
        Long alien = product("Alien", "12.99", 5);
        Long matrix = product("Matrix", "7.50", 2);

        List<ReservedLineDto> reserved = api.reserve(List.of(new StockLineDto(alien, 3), new StockLineDto(matrix, 2)));

        assertEquals(2, stockOf(alien));
        assertEquals(0, stockOf(matrix));
        assertEquals(new ReservedLineDto(alien, "Alien", new BigDecimal("12.99"), 3), reserved.get(0));
        assertEquals(new ReservedLineDto(matrix, "Matrix", new BigDecimal("7.50"), 2), reserved.get(1));
    }

    @Test
    void ifOneLineFailsStockStaysAsItWas() {
        Long alien = product("Alien", "12.99", 5);
        Long matrix = product("Matrix", "7.50", 1);

        assertThrows(InsufficientStockException.class,
                () -> api.reserve(List.of(new StockLineDto(alien, 2), new StockLineDto(matrix, 2))));

        assertEquals(5, stockOf(alien));
        assertEquals(1, stockOf(matrix));
    }

    @Test
    void unknownProductFailsAndTouchesNothingElse() {
        Long alien = product("Alien", "12.99", 5);

        assertThrows(UnknownProductException.class,
                () -> api.reserve(List.of(new StockLineDto(alien, 1), new StockLineDto(999L, 1))));

        assertEquals(5, stockOf(alien));
    }

    @Test
    void productWithoutPriceCannotBeReserved() {
        Long withoutPrice = product("No price", null, 5);

        assertThrows(ProductNotPurchasableException.class, () -> api.reserve(List.of(new StockLineDto(withoutPrice, 1))));

        assertEquals(5, stockOf(withoutPrice));
    }

    @Test
    void repeatedLineIsSummedBeforeCheckingStock() {
        Long alien = product("Alien", "12.99", 3);

        assertThrows(InsufficientStockException.class,
                () -> api.reserve(List.of(new StockLineDto(alien, 2), new StockLineDto(alien, 2))));

        assertEquals(3, stockOf(alien));
    }

    @Test
    void releaseReturnsUnitsAndReserveWorksAgain() {
        Long alien = product("Alien", "12.99", 2);
        api.reserve(List.of(new StockLineDto(alien, 2)));
        assertEquals(0, stockOf(alien));

        api.release(List.of(new StockLineDto(alien, 2)));

        assertEquals(2, stockOf(alien));
        api.reserve(List.of(new StockLineDto(alien, 2)));
        assertEquals(0, stockOf(alien));
    }

    @Test
    void releasingUnknownProductDoesNotFail() {
        api.release(List.of(new StockLineDto(999L, 1)));
    }
}

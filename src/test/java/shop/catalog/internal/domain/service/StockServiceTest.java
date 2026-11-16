package shop.catalog.internal.domain.service;

import shop.catalog.internal.domain.exception.InsufficientStockException;
import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.exception.MovieNotPurchasableException;
import shop.catalog.internal.domain.model.Movie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.catalog.internal.domain.fake.MovieRepositoryFake;
import shop.catalog.internal.domain.service.StockService.StockItem;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockServiceTest {

    private MovieRepositoryFake movies;
    private StockService service;
    private Long withStock;
    private Long unpriced;

    @BeforeEach
    void setUp() {
        movies = new MovieRepositoryFake();
        service = new StockServiceImpl(movies);
        withStock = movie("Alien", new BigDecimal("9.99"), 5);
        unpriced = movie("Silent", null, 5);
    }

    private Long movie(String title, BigDecimal price, int stock) {
        var m = new Movie();
        m.setTitle(title);
        m.setPrice(price);
        m.setStock(stock);
        return movies.save(m).getId();
    }

    private int stockOf(Long id) {
        return movies.findById(id).orElseThrow().getStock();
    }

    @Test
    void reservesAndSumsRepeatedLines() {
        var reserved = service.reserve(List.of(new StockItem(withStock, 2), new StockItem(withStock, 1)));

        assertEquals(2, reserved.size());
        assertEquals("Alien", reserved.get(0).title());
        assertEquals(2, stockOf(withStock));
    }

    @Test
    void failureLeavesTheStockUntouched() {
        assertThrows(MovieNotFoundException.class,
                () -> service.reserve(List.of(new StockItem(withStock, 1), new StockItem(99L, 1))));
        assertThrows(MovieNotPurchasableException.class,
                () -> service.reserve(List.of(new StockItem(withStock, 1), new StockItem(unpriced, 1))));
        assertThrows(InsufficientStockException.class, () -> service.reserve(List.of(new StockItem(withStock, 6))));
        assertEquals(5, stockOf(withStock));
    }

    @Test
    void releaseGivesUnitsBackAndIgnoresUnknownMovies() {
        service.reserve(List.of(new StockItem(withStock, 3)));

        service.release(List.of(new StockItem(withStock, 3), new StockItem(99L, 1)));

        assertEquals(5, stockOf(withStock));
    }
}

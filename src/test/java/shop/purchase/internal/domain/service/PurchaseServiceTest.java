package shop.purchase.internal.domain.service;

import shop.purchase.internal.domain.exception.EmptyCartException;
import shop.purchase.internal.domain.exception.MovieNoLongerAvailableException;
import shop.purchase.internal.domain.exception.MovieWithoutPriceException;
import shop.purchase.internal.domain.exception.PurchaseNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.purchase.internal.domain.fake.MovieCatalogFake;
import shop.purchase.internal.domain.fake.PurchaseLineRepositoryFake;
import shop.purchase.internal.domain.fake.PurchaseRepositoryFake;
import shop.purchase.internal.domain.fake.UserCartFake;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseServiceTest {

    private MovieCatalogFake movies;
    private UserCartFake carts;
    private PurchaseService service;

    @BeforeEach
    void setUp() {
        movies = new MovieCatalogFake();
        movies.createMovie(10L, "Matrix", new BigDecimal("7.99"));
        movies.createMovie(11L, "Priceless", null);
        carts = new UserCartFake();
        service = new PurchaseServiceImpl(new PurchaseRepositoryFake(), new PurchaseLineRepositoryFake(), carts, movies);
    }

    @Test
    void checkoutFreezesTitleAndPriceAndClearsTheCart() {
        carts.add(1L, 10L);
        var detail = service.checkout(1L);
        movies.changePrice(10L, new BigDecimal("99.00"));

        assertEquals(0, new BigDecimal("7.99").compareTo(detail.purchase().getTotal()));
        assertEquals("Matrix", service.detailOf(1L, detail.purchase().getId()).lines().get(0).getTitle());
        assertTrue(carts.findMovieIds(1L).isEmpty());
    }

    @Test
    void failedCheckoutLeavesTheCartIntact() {
        assertThrows(EmptyCartException.class, () -> service.checkout(1L));

        carts.add(1L, 10L);
        carts.add(1L, 99L);
        assertThrows(MovieNoLongerAvailableException.class, () -> service.checkout(1L));
        carts.add(2L, 11L);
        assertThrows(MovieWithoutPriceException.class, () -> service.checkout(2L));
        assertEquals(List.of(10L, 99L), carts.findMovieIds(1L));
    }

    @Test
    void someoneElsesPurchaseIsNotFound() {
        carts.add(1L, 10L);
        Long id = service.checkout(1L).purchase().getId();

        assertThrows(PurchaseNotFoundException.class, () -> service.detailOf(2L, id));
        assertEquals(1, service.historyOf(1L).size());
        assertTrue(service.historyOf(2L).isEmpty());
    }
}

package shop.cart.internal.domain.service;

import shop.cart.internal.domain.exception.MovieAlreadyInCartException;
import shop.cart.internal.domain.exception.MovieNotFoundException;
import shop.cart.internal.domain.exception.MovieNotInCartException;
import shop.cart.internal.domain.exception.MovieWithoutPriceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.cart.internal.domain.fake.CartLineRepositoryFake;
import shop.cart.internal.domain.fake.CartRepositoryFake;
import shop.cart.internal.domain.fake.MovieCatalogFake;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartServiceTest {

    private MovieCatalogFake movies;
    private CartService service;

    @BeforeEach
    void setUp() {
        movies = new MovieCatalogFake();
        movies.createMovie(10L, new BigDecimal("12.99"));
        movies.createMovie(11L, null);
        service = new CartServiceImpl(new CartRepositoryFake(), new CartLineRepositoryFake(), movies);
    }

    @Test
    void addsMoviesAndSumsLivePrices() {
        service.addMovie(1L, 10L);
        movies.changePrice(10L, new BigDecimal("15.00"));

        var contents = service.contentsOf(1L);

        assertEquals(List.of(10L), service.movieIdsOf(1L));
        assertEquals(0, new BigDecimal("15.00").compareTo(contents.total()));
    }

    @Test
    void rejectsUnknownUnpricedAndRepeatedMovies() {
        service.addMovie(1L, 10L);

        assertThrows(MovieNotFoundException.class, () -> service.addMovie(1L, 99L));
        assertThrows(MovieWithoutPriceException.class, () -> service.addMovie(1L, 11L));
        assertThrows(MovieAlreadyInCartException.class, () -> service.addMovie(1L, 10L));
    }

    @Test
    void removeFailsWithoutCartOrLineAndClearIsIdempotent() {
        assertThrows(MovieNotInCartException.class, () -> service.removeMovie(1L, 10L));
        service.clear(1L);

        service.addMovie(1L, 10L);
        service.clear(1L);

        assertTrue(service.movieIdsOf(1L).isEmpty());
        assertThrows(MovieNotInCartException.class, () -> service.removeMovie(1L, 10L));
    }
}

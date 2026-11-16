package shop.cart.internal.application.usecase;

import shop.cart.internal.application.dto.CartLineView;
import shop.cart.internal.application.dto.CartView;
import shop.cart.internal.application.facade.CartFacadeImpl;
import shop.cart.internal.domain.fake.CartLineRepositoryFake;
import shop.cart.internal.domain.fake.CartRepositoryFake;
import shop.cart.internal.domain.fake.MovieCatalogFake;

import shop.cart.internal.application.facade.CartFacadeImpl;
import shop.cart.internal.domain.model.Cart;
import shop.cart.internal.domain.service.CartServiceImpl;
import shop.cart.internal.domain.model.CartLine;
import shop.shared.error.ConflictException;
import shop.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartUseCasesTest {

    private static final BigDecimal PRICE = new BigDecimal("12.99");

    private CartRepositoryFake carts;
    private CartLineRepositoryFake lines;
    private MovieCatalogFake movies;
    private ViewCart viewCart;
    private AddToCart add;
    private RemoveFromCart remove;
    private ClearCart clear;
    private CartFacadeImpl api;

    @BeforeEach
    void setUp() {
        carts = new CartRepositoryFake();
        lines = new CartLineRepositoryFake();
        movies = new MovieCatalogFake();
        var service = new CartServiceImpl(carts, lines, movies);
        viewCart = new ViewCart(service);
        add = new AddToCart(service);
        remove = new RemoveFromCart(service);
        clear = new ClearCart(service);
        api = new CartFacadeImpl(service, clear);
    }

    private void createMovie(Long id, BigDecimal price) {
        movies.createMovie(id, price);
    }

    private void directLine(Long cartId, Long movieId) {
        var line = new CartLine();
        line.setCartId(cartId);
        line.setMovieId(movieId);
        lines.save(line);
    }

    @Test
    void viewWithoutCartReturnsEmptyCartWithZeroTotal() {
        CartView view = viewCart.execute(1L);

        assertTrue(view.lines().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(view.total()));
    }

    @Test
    void viewShowsLivePricePerMovieAndSumsTotal() {
        createMovie(10L, PRICE);
        createMovie(11L, new BigDecimal("5.00"));
        add.execute(1L, 10L);
        add.execute(1L, 11L);
        movies.changePrice(10L, new BigDecimal("15.50"));

        CartView view = viewCart.execute(1L);

        assertEquals(List.of(10L, 11L), view.lines().stream().map(CartLineView::movieId).toList());
        assertEquals(0, new BigDecimal("15.50").compareTo(view.lines().get(0).price()));
        assertEquals(0, new BigDecimal("5.00").compareTo(view.lines().get(1).price()));
        assertEquals(0, new BigDecimal("20.50").compareTo(view.total()));
    }

    @Test
    void viewReturnsOnlyUsersLines() {
        createMovie(10L, PRICE);
        createMovie(11L, PRICE);
        add.execute(1L, 10L);
        add.execute(2L, 11L);

        CartView view = viewCart.execute(1L);

        assertEquals(List.of("Película 10"), view.lines().stream().map(CartLineView::title).toList());
        assertEquals(1, view.lines().size());
    }

    @Test
    void addingFirstMovieCreatesCartAndReturnsLineWithCurrentPrice() {
        createMovie(10L, PRICE);
        assertTrue(carts.findByUserId(1L).isEmpty());

        CartLineView line = add.execute(1L, 10L);

        assertTrue(carts.findByUserId(1L).isPresent());
        assertEquals(10L, line.movieId());
        assertEquals("Película 10", line.title());
        assertEquals(0, PRICE.compareTo(line.price()));
        assertEquals(1, lines.count());
    }

    @Test
    void onCreationRaceTheOtherThreadsCartIsReread() {
        createMovie(10L, PRICE);
        var otherCart = new Cart();
        otherCart.setUserId(1L);
        carts.save(otherCart);
        carts.hideCartOnNextQuery(1L);

        CartLineView line = add.execute(1L, 10L);

        assertEquals(10L, line.movieId());
        assertEquals(0, PRICE.compareTo(line.price()));
        assertEquals(1, carts.count());
        assertEquals(1, lines.count());
        assertEquals(1, viewCart.execute(1L).lines().size());
    }

    @Test
    void addingMovieAlreadyInCartThrowsConflict() {
        createMovie(10L, PRICE);
        add.execute(1L, 10L);

        assertThrows(ConflictException.class, () -> add.execute(1L, 10L));

        assertEquals(1, lines.count());
    }

    @Test
    void addingUnknownMovieThrowsNotFoundAndCreatesNoCart() {
        assertThrows(ResourceNotFoundException.class, () -> add.execute(1L, 99L));

        assertEquals(0, carts.count());
        assertEquals(0, lines.count());
    }

    @Test
    void addingMovieWithoutPriceThrowsConflict() {
        createMovie(10L, null);

        assertThrows(ConflictException.class, () -> add.execute(1L, 10L));

        assertEquals(0, lines.count());
    }

    @Test
    void removingMovieNotInCartThrowsNotFound() {
        createMovie(10L, PRICE);

        assertThrows(ResourceNotFoundException.class, () -> remove.execute(1L, 10L));

        add.execute(1L, 10L);
        assertThrows(ResourceNotFoundException.class, () -> remove.execute(1L, 99L));
        assertEquals(1, lines.count());
    }

    @Test
    void removeDeletesLineAndLeavesCartEmptyButExisting() {
        createMovie(10L, PRICE);
        add.execute(1L, 10L);

        remove.execute(1L, 10L);

        assertEquals(0, lines.count());
        assertTrue(carts.findByUserId(1L).isPresent());
        assertTrue(viewCart.execute(1L).lines().isEmpty());
    }

    @Test
    void clearLeavesCartWithoutLinesAndIsIdempotent() {
        createMovie(10L, PRICE);
        createMovie(11L, PRICE);
        add.execute(1L, 10L);
        add.execute(1L, 11L);

        clear.execute(1L);
        assertEquals(0, lines.count());

        clear.execute(1L);
        assertTrue(carts.findByUserId(1L).isPresent());

        clear.execute(2L); // without a cart it does not fail either
        assertEquals(0, lines.count());
    }

    @Test
    void clearMarksCartAsModifiedSoOptimisticLockDetectsDoubleCheckout() {
        createMovie(10L, PRICE);
        add.execute(1L, 10L);
        assertNull(carts.findByUserId(1L).orElseThrow().getUpdatedAt());

        clear.execute(1L);

        assertNotNull(carts.findByUserId(1L).orElseThrow().getUpdatedAt());
    }

    @Test
    void orphanLineIsOmittedFromListingAndTotal() {
        createMovie(10L, PRICE);
        add.execute(1L, 10L);
        Long cartId = carts.findByUserId(1L).orElseThrow().getId();
        directLine(cartId, 99L); // orphan: movie 99 no longer exists

        CartView view = viewCart.execute(1L);

        assertEquals(List.of(10L), view.lines().stream().map(CartLineView::movieId).toList());
        assertEquals(0, PRICE.compareTo(view.total()));
    }

    @Test
    void moviesInCartReturnsIdsOfOwnLines() {
        createMovie(10L, PRICE);
        createMovie(11L, PRICE);
        add.execute(2L, 11L);
        add.execute(1L, 10L);

        assertEquals(List.of(10L), api.findMovieIdsInCart(1L));
        assertTrue(api.findMovieIdsInCart(3L).isEmpty());
    }
}

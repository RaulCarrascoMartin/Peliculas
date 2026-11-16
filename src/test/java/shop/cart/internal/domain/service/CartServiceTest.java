package shop.cart.internal.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import shop.cart.internal.domain.contract.MovieCatalog;
import shop.cart.internal.domain.dto.CartContentsDto;
import shop.cart.internal.domain.dto.CartDto;
import shop.cart.internal.domain.dto.CartLineDto;
import shop.cart.internal.domain.dto.CatalogMovieDto;
import shop.cart.internal.domain.dto.PricedLineDto;
import shop.cart.internal.domain.exception.CartAlreadyExistsException;
import shop.cart.internal.domain.exception.MovieAlreadyInCartException;
import shop.cart.internal.domain.exception.MovieNotFoundException;
import shop.cart.internal.domain.exception.MovieNotInCartException;
import shop.cart.internal.domain.exception.MovieWithoutPriceException;
import shop.cart.internal.domain.mapper.CartMapper;
import shop.cart.internal.domain.repository.CartRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Cart rules with the repository and the catalog mocked; the MapStruct mapper is the real one. */
class CartServiceTest {

    private static final BigDecimal PRICE = new BigDecimal("12.99");
    private static final long USER = 1L;
    private static final long CART = 50L;
    private static final CartDto STORED = new CartDto(CART, USER, 3L, null);

    private CartRepository carts;
    private MovieCatalog movies;
    private CartService service;

    @BeforeEach
    void setUp() {
        carts = mock(CartRepository.class);
        movies = mock(MovieCatalog.class);
        service = new CartServiceImpl(carts, movies, Mappers.getMapper(CartMapper.class));
    }

    private void movieInCatalog(long id, BigDecimal price) {
        when(movies.findByIds(List.of(id))).thenReturn(List.of(new CatalogMovieDto(id, "Película " + id, price)));
    }

    private void userHasCart() {
        when(carts.findByUserId(USER)).thenReturn(Optional.of(STORED));
    }

    private void cartHasLines(Long... movieIds) {
        List<CartLineDto> lines = new java.util.ArrayList<>();
        for (int i = 0; i < movieIds.length; i++) {
            lines.add(new CartLineDto(100L + i, CART, movieIds[i]));
        }
        when(carts.findLines(CART)).thenReturn(lines);
    }

    // ---------------------------------------------------------------- addMovie

    @Test
    void addingFirstMovieCreatesCartAndReturnsLineWithCurrentPrice() {
        movieInCatalog(10L, PRICE);
        when(carts.findByUserId(USER)).thenReturn(Optional.empty());
        when(carts.save(new CartDto(null, USER, null, null))).thenReturn(new CartDto(CART, USER, 0L, null));

        PricedLineDto line = service.addMovie(USER, 10L);

        assertEquals(new PricedLineDto(10L, "Película 10", PRICE), line);
        verify(carts).save(new CartDto(null, USER, null, null));
        verify(carts).addLine(new CartLineDto(null, CART, 10L));
    }

    @Test
    void addingToAnExistingCartOnlyAddsTheLine() {
        movieInCatalog(10L, PRICE);
        userHasCart();
        when(carts.existsLine(CART, 10L)).thenReturn(false);

        service.addMovie(USER, 10L);

        verify(carts, never()).save(any());
        verify(carts).addLine(new CartLineDto(null, CART, 10L));
    }

    @Test
    void onCreationRaceTheOtherThreadsCartIsReread() {
        movieInCatalog(10L, PRICE);
        when(carts.findByUserId(USER)).thenReturn(Optional.empty(), Optional.of(STORED));
        when(carts.save(any())).thenThrow(new CartAlreadyExistsException(USER));

        PricedLineDto line = service.addMovie(USER, 10L);

        assertEquals(10L, line.movieId());
        assertEquals(0, PRICE.compareTo(line.price()));
        verify(carts).addLine(new CartLineDto(null, CART, 10L));
    }

    @Test
    void addingMovieAlreadyInCartThrowsConflictAndAddsNothing() {
        movieInCatalog(10L, PRICE);
        userHasCart();
        when(carts.existsLine(CART, 10L)).thenReturn(true);

        var ex = assertThrows(MovieAlreadyInCartException.class, () -> service.addMovie(USER, 10L));

        assertTrue(ex.getMessage().contains("10"));
        verify(carts, never()).addLine(any());
    }

    @Test
    void addingUnknownMovieThrowsNotFoundAndCreatesNoCart() {
        when(movies.findByIds(List.of(99L))).thenReturn(List.of());

        var ex = assertThrows(MovieNotFoundException.class, () -> service.addMovie(USER, 99L));

        assertTrue(ex.getMessage().contains("99"));
        verifyNoInteractions(carts);
    }

    @Test
    void addingMovieWithoutPriceThrowsConflictAndCreatesNoCart() {
        movieInCatalog(11L, null);

        var ex = assertThrows(MovieWithoutPriceException.class, () -> service.addMovie(USER, 11L));

        assertTrue(ex.getMessage().contains("11"));
        verifyNoInteractions(carts);
    }

    @Test
    void aFreeMovieHasAPriceAndCanBeAdded() {
        movieInCatalog(12L, BigDecimal.ZERO);
        userHasCart();

        assertEquals(BigDecimal.ZERO, service.addMovie(USER, 12L).price());
    }

    // ---------------------------------------------------------------- removeMovie

    @Test
    void removeDeletesTheLineOfThatMovieAndLeavesTheCart() {
        userHasCart();
        when(carts.removeLine(CART, 10L)).thenReturn(true);

        service.removeMovie(USER, 10L);

        verify(carts).removeLine(CART, 10L);
        verify(carts, never()).removeLines(anyLong());
        verify(carts, never()).save(any());
    }

    @Test
    void removingWithoutCartThrowsNotFound() {
        when(carts.findByUserId(USER)).thenReturn(Optional.empty());

        var ex = assertThrows(MovieNotInCartException.class, () -> service.removeMovie(USER, 10L));

        assertTrue(ex.getMessage().contains("10"));
        verify(carts, never()).removeLine(anyLong(), anyLong());
    }

    @Test
    void removingMovieNotInCartThrowsNotFound() {
        userHasCart();
        when(carts.removeLine(CART, 99L)).thenReturn(false);

        var ex = assertThrows(MovieNotInCartException.class, () -> service.removeMovie(USER, 99L));

        assertTrue(ex.getMessage().contains("99"));
    }

    // ---------------------------------------------------------------- contentsOf

    @Test
    void viewWithoutCartReturnsEmptyCartWithZeroTotal() {
        when(carts.findByUserId(USER)).thenReturn(Optional.empty());

        CartContentsDto contents = service.contentsOf(USER);

        assertTrue(contents.lines().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(contents.total()));
        verifyNoInteractions(movies);
    }

    @Test
    void viewOfCartWithoutLinesIsEmptyWithZeroTotal() {
        userHasCart();
        cartHasLines();
        when(movies.findByIds(List.of())).thenReturn(List.of());

        assertEquals(new CartContentsDto(List.of(), BigDecimal.ZERO), service.contentsOf(USER));
    }

    @Test
    void viewShowsLivePricePerMovieInInsertionOrderAndSumsTotal() {
        userHasCart();
        cartHasLines(11L, 10L);
        // the catalog answers sorted by id, with the price of this moment
        when(movies.findByIds(List.of(11L, 10L))).thenReturn(List.of(
                new CatalogMovieDto(10L, "Alien", new BigDecimal("15.50")),
                new CatalogMovieDto(11L, "Matrix", new BigDecimal("5.00"))));

        CartContentsDto contents = service.contentsOf(USER);

        assertEquals(List.of(
                new PricedLineDto(11L, "Matrix", new BigDecimal("5.00")),
                new PricedLineDto(10L, "Alien", new BigDecimal("15.50"))), contents.lines());
        assertEquals(new BigDecimal("20.50"), contents.total());
    }

    @Test
    void viewReadsOnlyTheLinesOfTheUsersCart() {
        userHasCart();
        cartHasLines(10L);
        movieInCatalog(10L, PRICE);

        service.contentsOf(USER);

        verify(carts).findByUserId(USER);
        verify(carts).findLines(CART);
    }

    @Test
    void orphanLineIsOmittedFromListingAndTotal() {
        userHasCart();
        cartHasLines(10L, 99L); // orphan: movie 99 no longer exists
        when(movies.findByIds(List.of(10L, 99L))).thenReturn(List.of(new CatalogMovieDto(10L, "Alien", PRICE)));

        CartContentsDto contents = service.contentsOf(USER);

        assertEquals(List.of(10L), contents.lines().stream().map(PricedLineDto::movieId).toList());
        assertEquals(0, PRICE.compareTo(contents.total()));
    }

    // ---------------------------------------------------------------- movieIdsOf

    @Test
    void movieIdsAreThoseOfTheOwnLinesInInsertionOrder() {
        userHasCart();
        cartHasLines(12L, 10L);

        assertEquals(List.of(12L, 10L), service.movieIdsOf(USER));
    }

    @Test
    void movieIdsWithoutCartAreEmpty() {
        when(carts.findByUserId(3L)).thenReturn(Optional.empty());

        assertTrue(service.movieIdsOf(3L).isEmpty());
        verify(carts, never()).findLines(anyLong());
    }

    // ---------------------------------------------------------------- clear

    @Test
    void clearRemovesTheLinesAndThenTouchesTheCartWithTheVersionItRead() {
        userHasCart();
        LocalDateTime before = LocalDateTime.now();

        service.clear(USER);

        InOrder order = inOrder(carts);
        order.verify(carts).removeLines(CART);
        ArgumentCaptor<CartDto> saved = ArgumentCaptor.forClass(CartDto.class);
        order.verify(carts).save(saved.capture());
        // same id and version as read: the repository's optimistic lock detects the double checkout
        assertEquals(CART, saved.getValue().id());
        assertEquals(USER, saved.getValue().userId());
        assertEquals(3L, saved.getValue().version());
        assertNotNull(saved.getValue().updatedAt());
        assertTrue(!saved.getValue().updatedAt().isBefore(before));
    }

    @Test
    void clearOfAnAlreadyEmptyCartStillTouchesIt() {
        when(carts.findByUserId(USER)).thenReturn(Optional.of(new CartDto(CART, USER, 4L, LocalDateTime.now())));

        service.clear(USER);

        verify(carts).removeLines(CART);
        verify(carts).save(any());
    }

    @Test
    void clearWithoutCartDoesNothing() {
        when(carts.findByUserId(2L)).thenReturn(Optional.empty());

        service.clear(2L);

        verify(carts, never()).removeLines(anyLong());
        verify(carts, never()).save(any());
    }
}

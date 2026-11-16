package shop.cart.internal.persistence.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import shop.cart.internal.domain.dto.CartDto;
import shop.cart.internal.domain.dto.CartLineDto;
import shop.cart.internal.domain.exception.CartAlreadyExistsException;
import shop.cart.internal.domain.exception.MovieAlreadyInCartException;
import shop.cart.internal.domain.repository.CartRepository;
import shop.shared.MariaDbRepositoryTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SQL and row ↔ DTO mapping of carts and their lines against a real MariaDB. */
@Import(CartRepositoryImpl.class)
class CartRepositoryImplTest extends MariaDbRepositoryTest {

    private static final LocalDateTime CLEARED_AT = LocalDateTime.of(2026, 11, 2, 10, 15, 30);

    @Autowired
    private CartRepository repository;

    @Autowired
    private JdbcClient jdbc;

    private static CartDto cart(Long userId) {
        return new CartDto(null, userId, null, null);
    }

    private static CartLineDto line(Long cartId, Long movieId) {
        return new CartLineDto(null, cartId, movieId);
    }

    private long carts() {
        return jdbc.sql("select count(*) from carritos").query(Long.class).single();
    }

    private long lines() {
        return jdbc.sql("select count(*) from lineas_carrito").query(Long.class).single();
    }

    private List<Long> moviesOf(Long cartId) {
        return repository.findLines(cartId).stream().map(CartLineDto::movieId).toList();
    }

    // ---------------------------------------------------------------- cart

    @Test
    void saveAssignsIdAndVersionZeroAndFindByUserIdReturnsEveryColumn() {
        CartDto saved = repository.save(new CartDto(null, 1L, null, CLEARED_AT));

        assertNotNull(saved.id());
        assertEquals(0L, saved.version());
        CartDto found = repository.findByUserId(1L).orElseThrow();
        assertEquals(saved, found);
        assertEquals(1L, found.userId());
        assertEquals(CLEARED_AT, found.updatedAt());
    }

    @Test
    void newCartHasNoClearingDate() {
        repository.save(cart(1L));

        assertNull(repository.findByUserId(1L).orElseThrow().updatedAt());
    }

    @Test
    void findsOnlyTheCartOfThatUser() {
        repository.save(cart(1L));
        repository.save(cart(2L));

        assertEquals(1L, repository.findByUserId(1L).orElseThrow().userId());
        assertTrue(repository.findByUserId(3L).isEmpty());
    }

    @Test
    void secondCartForTheSameUserIsTranslatedAndLeavesNoRow() {
        repository.save(cart(1L));

        var ex = assertThrows(CartAlreadyExistsException.class, () -> repository.save(cart(1L)));

        assertTrue(ex.getMessage().contains("1"));
        assertEquals(1, carts());
    }

    @Test
    void userIsRequired() {
        assertThrows(DataIntegrityViolationException.class, () -> repository.save(cart(null)));

        assertEquals(0, carts());
    }

    @Test
    void updateStoresTheColumnsAndIncrementsTheVersion() {
        CartDto saved = repository.save(cart(1L));

        CartDto updated = repository.save(new CartDto(saved.id(), 1L, saved.version(), CLEARED_AT));

        assertEquals(1L, updated.version());
        CartDto found = repository.findByUserId(1L).orElseThrow();
        assertEquals(updated, found);
        assertEquals(CLEARED_AT, found.updatedAt());
    }

    @Test
    void updatingWithAStaleVersionFailsWithOptimisticLockAndChangesNothing() {
        CartDto saved = repository.save(cart(1L));
        CartDto stale = repository.findByUserId(1L).orElseThrow();
        repository.save(saved);

        assertThrows(OptimisticLockingFailureException.class,
                () -> repository.save(new CartDto(stale.id(), 1L, stale.version(), CLEARED_AT)));

        CartDto found = repository.findByUserId(1L).orElseThrow();
        assertEquals(1L, found.version());
        assertNull(found.updatedAt());
    }

    @Test
    void updatingAnUnknownCartFailsWithOptimisticLock() {
        assertThrows(OptimisticLockingFailureException.class,
                () -> repository.save(new CartDto(987654L, 1L, 0L, null)));
    }

    // ---------------------------------------------------------------- lines

    @Test
    void addLineAssignsIdAndTheLineIsReadWithEveryColumn() {
        CartDto cart = repository.save(cart(1L));

        CartLineDto added = repository.addLine(line(cart.id(), 10L));

        assertNotNull(added.id());
        assertEquals(List.of(new CartLineDto(added.id(), cart.id(), 10L)), repository.findLines(cart.id()));
    }

    @Test
    void listsCartLinesInInsertionOrderWithoutMixingCarts() {
        repository.addLine(line(1L, 12L));
        repository.addLine(line(1L, 10L));
        repository.addLine(line(2L, 99L));

        assertEquals(List.of(12L, 10L), moviesOf(1L));
        assertEquals(List.of(99L), moviesOf(2L));
        assertTrue(repository.findLines(3L).isEmpty());
    }

    @Test
    void existsLineOfAMovieInACart() {
        repository.addLine(line(1L, 10L));
        repository.addLine(line(2L, 11L));

        assertTrue(repository.existsLine(1L, 10L));
        assertFalse(repository.existsLine(1L, 11L));
        assertFalse(repository.existsLine(3L, 10L));
    }

    @Test
    void secondLineOfTheSameMovieInTheSameCartIsTranslatedAndLeavesNoRow() {
        repository.addLine(line(1L, 10L));

        var ex = assertThrows(MovieAlreadyInCartException.class, () -> repository.addLine(line(1L, 10L)));

        assertTrue(ex.getMessage().contains("10"));
        assertEquals(1, lines());
    }

    @Test
    void sameMovieCanBeInDifferentCarts() {
        repository.addLine(line(1L, 10L));
        repository.addLine(line(2L, 10L));

        assertEquals(2, lines());
    }

    @Test
    void lineWithoutMovieIsRejected() {
        assertThrows(DataIntegrityViolationException.class, () -> repository.addLine(line(1L, null)));

        assertEquals(0, lines());
    }

    @Test
    void removeLineDeletesOnlyThatMovieOfThatCart() {
        repository.addLine(line(1L, 10L));
        repository.addLine(line(1L, 11L));
        repository.addLine(line(2L, 10L));

        assertTrue(repository.removeLine(1L, 10L));

        assertEquals(List.of(11L), moviesOf(1L));
        assertEquals(List.of(10L), moviesOf(2L));
    }

    @Test
    void removeLineOfAMovieThatIsNotInTheCartAffectsNothing() {
        repository.addLine(line(1L, 10L));

        assertFalse(repository.removeLine(1L, 99L));
        assertFalse(repository.removeLine(2L, 10L));

        assertEquals(1, lines());
    }

    @Test
    void removeLinesDeletesAllLinesOfThatCartWithoutTouchingTheCartRow() {
        CartDto cart = repository.save(cart(1L));
        repository.addLine(line(cart.id(), 10L));
        repository.addLine(line(cart.id(), 11L));
        repository.addLine(line(cart.id() + 1, 12L));

        repository.removeLines(cart.id());

        assertTrue(repository.findLines(cart.id()).isEmpty());
        assertEquals(List.of(12L), moviesOf(cart.id() + 1));
        assertEquals(cart, repository.findByUserId(1L).orElseThrow());
    }

    @Test
    void removeLinesOfACartWithoutLinesDoesNothing() {
        repository.addLine(line(1L, 10L));

        repository.removeLines(2L);

        assertEquals(1, lines());
    }
}

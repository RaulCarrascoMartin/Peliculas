package shop.cart.internal.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class CartLineRepositoryTest {

    @Autowired
    private CartLineRepository repository;

    private CartLine line(Long cartId, Long movieId) {
        var line = new CartLine();
        line.setCartId(cartId);
        line.setMovieId(movieId);
        return line;
    }

    @Test
    void savesLineAndAssignsId() {
        var saved = repository.saveAndFlush(line(1L, 10L));

        assertEquals(10L, saved.getMovieId());
        assertEquals(1L, saved.getCartId());
    }

    @Test
    void findsLineOfAMovieInACart() {
        repository.saveAndFlush(line(1L, 10L));
        repository.saveAndFlush(line(1L, 11L));
        repository.saveAndFlush(line(2L, 10L));

        assertEquals(10L, repository.findByCartIdAndMovieId(1L, 10L).orElseThrow().getMovieId());
        assertTrue(repository.findByCartIdAndMovieId(1L, 12L).isEmpty());
    }

    @Test
    void listsCartLinesInInsertionOrder() {
        repository.saveAndFlush(line(1L, 12L));
        repository.saveAndFlush(line(1L, 10L));
        repository.saveAndFlush(line(2L, 99L));

        List<Long> movies = repository.findByCartIdOrderById(1L).stream()
                .map(CartLine::getMovieId).toList();

        assertEquals(List.of(12L, 10L), movies);
    }

    @Test
    void doesNotAllowTwoLinesOfSameMovieInSameCart() {
        repository.saveAndFlush(line(1L, 10L));

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(line(1L, 10L)));
    }

    @Test
    void sameMovieCanBeInDifferentCarts() {
        repository.saveAndFlush(line(1L, 10L));
        repository.saveAndFlush(line(2L, 10L));

        assertEquals(2, repository.count());
    }

    @Test
    void deletesAllLinesOfAMovieInAllCarts() {
        repository.saveAndFlush(line(1L, 10L));
        repository.saveAndFlush(line(2L, 10L));
        repository.saveAndFlush(line(1L, 11L));

        repository.deleteByMovieId(10L);

        assertEquals(1, repository.count());
        assertTrue(repository.findByCartIdAndMovieId(1L, 10L).isEmpty());
        assertTrue(repository.findByCartIdAndMovieId(2L, 10L).isEmpty());
    }

    @Test
    void deletesAllLinesOfACart() {
        repository.saveAndFlush(line(1L, 10L));
        repository.saveAndFlush(line(1L, 11L));
        repository.saveAndFlush(line(2L, 12L));

        repository.deleteByCartId(1L);

        assertEquals(1, repository.count());
        assertTrue(repository.findByCartIdAndMovieId(1L, 10L).isEmpty());
        assertTrue(repository.findByCartIdAndMovieId(1L, 11L).isEmpty());
    }
}

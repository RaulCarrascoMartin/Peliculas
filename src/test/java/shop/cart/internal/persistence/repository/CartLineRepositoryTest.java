package shop.cart.internal.persistence.repository;

import shop.cart.internal.persistence.entity.CartLineEntity;

import shop.cart.internal.domain.model.CartLine;
import shop.cart.internal.domain.repository.CartLineRepository;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(CartLineRepositoryImpl.class)
class CartLineRepositoryTest {

    @Autowired
    private CartLineRepository repository;

    @Autowired
    private EntityManager em;

    private long count() {
        return em.createQuery("select count(e) from CartLineEntity e", Long.class).getSingleResult();
    }

    private <T> T flushed(T saved) {
        em.flush();
        return saved;
    }

    private CartLine line(Long cartId, Long movieId) {
        var line = new CartLine();
        line.setCartId(cartId);
        line.setMovieId(movieId);
        return line;
    }

    @Test
    void savesLineAndAssignsId() {
        var saved = flushed(repository.save(line(1L, 10L)));

        assertEquals(10L, saved.getMovieId());
        assertEquals(1L, saved.getCartId());
    }

    @Test
    void findsLineOfAMovieInACart() {
        flushed(repository.save(line(1L, 10L)));
        flushed(repository.save(line(1L, 11L)));
        flushed(repository.save(line(2L, 10L)));

        assertEquals(10L, repository.findByCartIdAndMovieId(1L, 10L).orElseThrow().getMovieId());
        assertTrue(repository.findByCartIdAndMovieId(1L, 12L).isEmpty());
    }

    @Test
    void listsCartLinesInInsertionOrder() {
        flushed(repository.save(line(1L, 12L)));
        flushed(repository.save(line(1L, 10L)));
        flushed(repository.save(line(2L, 99L)));

        List<Long> movies = repository.findByCartIdOrderById(1L).stream()
                .map(CartLine::getMovieId).toList();

        assertEquals(List.of(12L, 10L), movies);
    }

    @Test
    void doesNotAllowTwoLinesOfSameMovieInSameCart() {
        flushed(repository.save(line(1L, 10L)));

        assertThrows(DataIntegrityViolationException.class, () -> flushed(repository.save(line(1L, 10L))));
    }

    @Test
    void sameMovieCanBeInDifferentCarts() {
        flushed(repository.save(line(1L, 10L)));
        flushed(repository.save(line(2L, 10L)));

        assertEquals(2, count());
    }

    @Test
    void deletesAllLinesOfAMovieInAllCarts() {
        flushed(repository.save(line(1L, 10L)));
        flushed(repository.save(line(2L, 10L)));
        flushed(repository.save(line(1L, 11L)));

        repository.deleteByMovieId(10L);

        assertEquals(1, count());
        assertTrue(repository.findByCartIdAndMovieId(1L, 10L).isEmpty());
        assertTrue(repository.findByCartIdAndMovieId(2L, 10L).isEmpty());
    }

    @Test
    void deletesAllLinesOfACart() {
        flushed(repository.save(line(1L, 10L)));
        flushed(repository.save(line(1L, 11L)));
        flushed(repository.save(line(2L, 12L)));

        repository.deleteByCartId(1L);

        assertEquals(1, count());
        assertTrue(repository.findByCartIdAndMovieId(1L, 10L).isEmpty());
        assertTrue(repository.findByCartIdAndMovieId(1L, 11L).isEmpty());
    }
}

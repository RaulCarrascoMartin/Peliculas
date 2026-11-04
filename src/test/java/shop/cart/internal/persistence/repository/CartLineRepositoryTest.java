package shop.cart.internal.persistence.repository;


import shop.cart.internal.domain.model.CartLine;
import shop.cart.internal.domain.repository.CartLineRepository;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcCartLineRepository.class)
class CartLineRepositoryTest {

    @Autowired
    private CartLineRepository repository;

    @Autowired
    private JdbcClient jdbc;

    private long count() {
        return jdbc.sql("select count(*) from lineas_carrito").query(Long.class).single();
    }


    private CartLine line(Long cartId, Long movieId) {
        var line = new CartLine();
        line.setCartId(cartId);
        line.setMovieId(movieId);
        return line;
    }

    @Test
    void savesLineAndAssignsId() {
        var saved = repository.save(line(1L, 10L));

        assertEquals(10L, saved.getMovieId());
        assertEquals(1L, saved.getCartId());
    }

    @Test
    void findsLineOfAMovieInACart() {
        repository.save(line(1L, 10L));
        repository.save(line(1L, 11L));
        repository.save(line(2L, 10L));

        assertEquals(10L, repository.findByCartIdAndMovieId(1L, 10L).orElseThrow().getMovieId());
        assertTrue(repository.findByCartIdAndMovieId(1L, 12L).isEmpty());
    }

    @Test
    void listsCartLinesInInsertionOrder() {
        repository.save(line(1L, 12L));
        repository.save(line(1L, 10L));
        repository.save(line(2L, 99L));

        List<Long> movies = repository.findByCartIdOrderById(1L).stream()
                .map(CartLine::getMovieId).toList();

        assertEquals(List.of(12L, 10L), movies);
    }

    @Test
    void doesNotAllowTwoLinesOfSameMovieInSameCart() {
        repository.save(line(1L, 10L));

        assertThrows(DataIntegrityViolationException.class, () -> repository.save(line(1L, 10L)));
    }

    @Test
    void sameMovieCanBeInDifferentCarts() {
        repository.save(line(1L, 10L));
        repository.save(line(2L, 10L));

        assertEquals(2, count());
    }

    @Test
    void deletesAllLinesOfAMovieInAllCarts() {
        repository.save(line(1L, 10L));
        repository.save(line(2L, 10L));
        repository.save(line(1L, 11L));

        repository.deleteByMovieId(10L);

        assertEquals(1, count());
        assertTrue(repository.findByCartIdAndMovieId(1L, 10L).isEmpty());
        assertTrue(repository.findByCartIdAndMovieId(2L, 10L).isEmpty());
    }

    @Test
    void deletesAllLinesOfACart() {
        repository.save(line(1L, 10L));
        repository.save(line(1L, 11L));
        repository.save(line(2L, 12L));

        repository.deleteByCartId(1L);

        assertEquals(1, count());
        assertTrue(repository.findByCartIdAndMovieId(1L, 10L).isEmpty());
        assertTrue(repository.findByCartIdAndMovieId(1L, 11L).isEmpty());
    }
}

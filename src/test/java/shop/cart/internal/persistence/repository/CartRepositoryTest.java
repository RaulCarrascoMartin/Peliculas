package shop.cart.internal.persistence.repository;

import shop.cart.internal.domain.model.Cart;
import shop.cart.internal.domain.exception.CartAlreadyExistsException;
import shop.cart.internal.domain.repository.CartRepository;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcCartRepository.class)
class CartRepositoryTest {

    @Autowired
    private CartRepository repository;



    private Cart cart(Long userId) {
        var cart = new Cart();
        cart.setUserId(userId);
        return cart;
    }

    @Test
    void savesCartAndAssignsId() {
        assertNotNull(repository.save(cart(1L)).getId());
    }

    @Test
    void findsCartOfAUser() {
        repository.save(cart(1L));
        repository.save(cart(2L));

        var own = repository.findByUserId(1L).orElseThrow();

        assertEquals(1L, own.getUserId());
        assertTrue(repository.findByUserId(3L).isEmpty());
    }

    @Test
    void doesNotAllowTwoCartsForSameUser() {
        repository.save(cart(1L));

        assertThrows(CartAlreadyExistsException.class, () -> repository.save(cart(1L)));
    }

    @Test
    void userIsRequired() {
        assertThrows(DataIntegrityViolationException.class, () -> repository.save(cart(null)));
    }

    @Test
    void versionStartsAndChangesOnUpdate() {
        var saved = repository.save(cart(1L));
        assertEquals(0L, saved.getVersion());

        saved.setUserId(2L);
        repository.save(saved);

        assertEquals(1L, repository.findByUserId(2L).orElseThrow().getVersion());
    }

    @Test
    void updatingWithAStaleVersionFailsWithOptimisticLock() {
        var saved = repository.save(cart(1L));
        var stale = repository.findByUserId(1L).orElseThrow();

        repository.save(saved);

        assertThrows(OptimisticLockingFailureException.class, () -> repository.save(stale));
    }
}

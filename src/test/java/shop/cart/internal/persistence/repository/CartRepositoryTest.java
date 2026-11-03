package shop.cart.internal.persistence.repository;

import shop.cart.internal.domain.model.Cart;
import shop.cart.internal.domain.exception.CartAlreadyExistsException;
import shop.cart.internal.domain.repository.CartRepository;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(CartRepositoryImpl.class)
class CartRepositoryTest {

    @Autowired
    private CartRepository repository;

    @Autowired
    private EntityManager em;

    private <T> T flushed(T saved) {
        em.flush();
        return saved;
    }

    private Cart cart(Long userId) {
        var cart = new Cart();
        cart.setUserId(userId);
        return cart;
    }

    @Test
    void savesCartAndAssignsId() {
        assertNotNull(flushed(repository.save(cart(1L))).getId());
    }

    @Test
    void findsCartOfAUser() {
        flushed(repository.save(cart(1L)));
        flushed(repository.save(cart(2L)));

        var own = repository.findByUserId(1L).orElseThrow();

        assertEquals(1L, own.getUserId());
        assertTrue(repository.findByUserId(3L).isEmpty());
    }

    @Test
    void doesNotAllowTwoCartsForSameUser() {
        flushed(repository.save(cart(1L)));

        assertThrows(CartAlreadyExistsException.class, () -> flushed(repository.save(cart(1L))));
    }

    @Test
    void userIsRequired() {
        assertThrows(DataIntegrityViolationException.class, () -> flushed(repository.save(cart(null))));
    }

    @Test
    void versionStartsAndChangesOnUpdate() {
        var saved = flushed(repository.save(cart(1L)));
        assertEquals(0L, saved.getVersion());

        saved.setUserId(2L);
        flushed(repository.save(saved));
        em.clear();

        assertEquals(1L, repository.findByUserId(2L).orElseThrow().getVersion());
    }
}

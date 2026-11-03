package shop.cart.internal.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
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
        assertNotNull(repository.saveAndFlush(cart(1L)).getId());
    }

    @Test
    void findsCartOfAUser() {
        repository.saveAndFlush(cart(1L));
        repository.saveAndFlush(cart(2L));

        var own = repository.findByUserId(1L).orElseThrow();

        assertEquals(1L, own.getUserId());
        assertTrue(repository.findByUserId(3L).isEmpty());
    }

    @Test
    void doesNotAllowTwoCartsForSameUser() {
        repository.saveAndFlush(cart(1L));

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(cart(1L)));
    }

    @Test
    void userIsRequired() {
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(cart(null)));
    }

    @Test
    void versionStartsAndChangesOnUpdate() {
        var saved = repository.saveAndFlush(cart(1L));
        assertEquals(0L, saved.getVersion());

        saved.setUserId(2L);
        repository.saveAndFlush(saved);

        assertEquals(1L, saved.getVersion());
    }
}

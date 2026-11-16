package shop.purchase.internal.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import shop.cart.api.facade.CartFacade;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The adapter delegates to the cart's facade and translates nothing: neither types nor failures. */
class UserCartAdapterTest {

    @Test
    void delegatesToTheCartFacade() {
        List<Long> cleared = new ArrayList<>();
        var adapter = new UserCartAdapter(new CartFacade() {
            @Override
            public List<Long> findMovieIdsInCart(Long userId) {
                return List.of(10L, 11L);
            }

            @Override
            public void clear(Long userId) {
                cleared.add(userId);
            }
        });

        assertEquals(List.of(10L, 11L), adapter.findMovieIds(1L));
        adapter.clear(1L);
        assertEquals(List.of(1L), cleared);
    }

    @Test
    void lostOptimisticLockOfTheCartIsNotSwallowedNorTranslated() {
        var lost = new OptimisticLockingFailureException("cart updated by another transaction");
        var adapter = new UserCartAdapter(new CartFacade() {
            @Override
            public List<Long> findMovieIdsInCart(Long userId) {
                return List.of();
            }

            @Override
            public void clear(Long userId) {
                throw lost;
            }
        });

        assertSame(lost, assertThrows(OptimisticLockingFailureException.class, () -> adapter.clear(1L)));
    }
}

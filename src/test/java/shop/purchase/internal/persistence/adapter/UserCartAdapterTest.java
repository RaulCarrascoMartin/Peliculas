package shop.purchase.internal.persistence.adapter;

import org.junit.jupiter.api.Test;
import shop.cart.api.facade.CartFacade;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

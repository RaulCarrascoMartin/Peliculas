package shop.cart.internal.application.facade;

import org.junit.jupiter.api.Test;
import shop.cart.internal.domain.service.CartService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The facade only delegates to the service. */
class CartFacadeImplTest {

    private final CartService service = mock(CartService.class);
    private final CartFacadeImpl facade = new CartFacadeImpl(service);

    @Test
    void movieIdsInCartAreThoseOfTheService() {
        when(service.movieIdsOf(1L)).thenReturn(List.of(12L, 10L));

        assertEquals(List.of(12L, 10L), facade.findMovieIdsInCart(1L));
    }

    @Test
    void clearDelegatesToTheService() {
        facade.clear(1L);

        verify(service).clear(1L);
    }
}

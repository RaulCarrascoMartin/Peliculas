package shop.cart.internal.usecase;

import shop.cart.internal.domain.Cart;
import shop.cart.internal.domain.CartRepository;
import shop.shared.InMemoryRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

public class CartRepositoryFake extends InMemoryRepository<Cart> implements CartRepository {

    /** userId whose cart the next query returns empty: simulates the read in the middle of a race. */
    private Long hideOnNextQuery;

    public void hideCartOnNextQuery(Long userId) {
        hideOnNextQuery = userId;
    }

    @Override
    protected Long idOf(Cart cart) {
        return cart.getId();
    }

    @Override
    protected void assignId(Cart cart, Long id) {
        cart.setId(id);
    }

    @Override
    public <S extends Cart> S save(S cart) {
        // Simulates the UNIQUE(user_id) that the real double enforces when persisting a new cart.
        if (idOf(cart) == null && entities.values().stream()
                .anyMatch(c -> c.getUserId().equals(cart.getUserId()))) {
            throw new DataIntegrityViolationException("Repeated cart for the same user");
        }
        return super.save(cart);
    }

    @Override
    public Optional<Cart> findByUserId(Long userId) {
        Optional<Cart> found = entities.values().stream()
                .filter(c -> c.getUserId().equals(userId))
                .findFirst();
        if (userId.equals(hideOnNextQuery)) {
            hideOnNextQuery = null;
            return Optional.empty();
        }
        return found;
    }
}

package shop.purchase.internal.usecase;

import shop.cart.api.CartApi;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Test double of {@link CartApi}: carts as lists of movie ids per user. */
public class CartApiFake implements CartApi {

    private final Map<Long, List<Long>> carts = new HashMap<>();
    private boolean failOnClearWithOptimisticLock;

    public void add(Long userId, Long movieId) {
        carts.computeIfAbsent(userId, id -> new ArrayList<>()).add(movieId);
    }

    /** Simulates the second simultaneous checkout losing the {@code @Version} race on clear. */
    public void failOnClearWithOptimisticLock() {
        failOnClearWithOptimisticLock = true;
    }

    @Override
    public List<Long> findMovieIdsInCart(Long userId) {
        return List.copyOf(carts.getOrDefault(userId, List.of()));
    }

    @Override
    public void clear(Long userId) {
        if (failOnClearWithOptimisticLock) {
            throw new ObjectOptimisticLockingFailureException("Cart", userId);
        }
        carts.remove(userId);
    }
}

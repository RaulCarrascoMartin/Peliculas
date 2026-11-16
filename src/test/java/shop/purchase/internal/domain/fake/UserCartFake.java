package shop.purchase.internal.domain.fake;

import shop.purchase.internal.domain.contract.UserCart;

import org.springframework.dao.OptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Test double of {@link UserCart}: carts as lists of movie ids per user. */
public class UserCartFake implements UserCart {

    private final Map<Long, List<Long>> carts = new HashMap<>();

    private boolean failOnClearWithOptimisticLock;

    /** Simulates the second simultaneous checkout losing the {@code @Version} race on the cart. */
    public void failOnClearWithOptimisticLock() {
        failOnClearWithOptimisticLock = true;
    }

    public void add(Long userId, Long movieId) {
        carts.computeIfAbsent(userId, id -> new ArrayList<>()).add(movieId);
    }

    @Override
    public List<Long> findMovieIds(Long userId) {
        return List.copyOf(carts.getOrDefault(userId, List.of()));
    }

    @Override
    public void clear(Long userId) {
        if (failOnClearWithOptimisticLock) {
            throw new OptimisticLockingFailureException("Cart of user " + userId + " was updated by another transaction");
        }
        carts.remove(userId);
    }
}

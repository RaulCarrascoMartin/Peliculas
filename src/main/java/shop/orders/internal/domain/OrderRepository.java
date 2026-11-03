package shop.orders.internal.domain;

import java.util.Optional;

/** Order persistence; the implementation uses JDBC and lives in {@code internal.persistence}. */
public interface OrderRepository {

    /** Inserts the order and its lines and returns the order with the assigned id. */
    Order save(Order order);

    Optional<Order> findById(Long id);

    /** Moves to CANCELLED only if it is still CREATED; returns {@code false} if there was nothing to cancel. */
    boolean cancelIfCreated(Long id);
}

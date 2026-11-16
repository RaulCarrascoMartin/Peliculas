package shop.orders.internal.domain.repository;

import shop.orders.internal.domain.dto.OrderDto;

import java.util.Optional;

/** Order persistence; the implementation uses JDBC and lives in {@code internal.persistence}. */
public interface OrderRepository {

    /** Inserts the order and its lines and returns the order with the assigned id. */
    OrderDto save(OrderDto order);

    Optional<OrderDto> findById(Long id);

    /** Moves to CANCELLED only if it is still CREATED; returns {@code false} if there was nothing to cancel. */
    boolean cancelIfCreated(Long id);
}

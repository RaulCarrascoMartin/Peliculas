package shop.orders.internal.domain.exception;

import shop.shared.error.ConflictException;

/** The order is not in the CREATED state, so it cannot be cancelled (409). */
public class OrderCannotBeCancelledException extends ConflictException {

    public OrderCannotBeCancelledException(Long orderId) {
        super("El pedido " + orderId + " no está en estado CREATED");
    }
}

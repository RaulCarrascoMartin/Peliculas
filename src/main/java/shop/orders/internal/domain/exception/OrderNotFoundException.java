package shop.orders.internal.domain.exception;

import shop.shared.error.ResourceNotFoundException;

/** The order does not exist (404). */
public class OrderNotFoundException extends ResourceNotFoundException {

    public OrderNotFoundException(Long orderId) {
        super("El pedido " + orderId + " no existe");
    }
}

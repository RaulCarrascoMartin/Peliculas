package shop.orders.internal.domain.exception;

import shop.shared.error.ConflictException;

/** There are not enough units of a product to fulfil the order (409). */
public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(Long productId) {
        super("No hay stock suficiente del producto " + productId);
    }
}

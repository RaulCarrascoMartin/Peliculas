package shop.orders.internal.domain.exception;

import shop.shared.error.ConflictException;

/** The product exists but has no price, so it cannot be ordered (409). */
public class ProductNotPurchasableException extends ConflictException {

    public ProductNotPurchasableException(Long productId) {
        super("El producto " + productId + " no tiene precio y no es comprable");
    }
}

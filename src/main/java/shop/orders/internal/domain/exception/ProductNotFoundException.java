package shop.orders.internal.domain.exception;

import shop.shared.error.ResourceNotFoundException;

/** A line of the order references a product that is not in the catalog (404). */
public class ProductNotFoundException extends ResourceNotFoundException {

    public ProductNotFoundException(Long productId) {
        super("El producto " + productId + " no existe");
    }
}

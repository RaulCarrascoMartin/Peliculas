package shop.purchase.internal.domain.exception;

import shop.shared.error.ConflictException;

/** The cart has no lines, so there is nothing to buy (409). */
public class EmptyCartException extends ConflictException {

    public EmptyCartException() {
        super("El carrito está vacío");
    }
}

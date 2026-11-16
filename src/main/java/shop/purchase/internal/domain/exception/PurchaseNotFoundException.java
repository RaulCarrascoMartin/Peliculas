package shop.purchase.internal.domain.exception;

import shop.shared.error.ResourceNotFoundException;

/** The purchase does not exist or belongs to someone else: both cases are a 404, never a 403. */
public class PurchaseNotFoundException extends ResourceNotFoundException {

    public PurchaseNotFoundException(Long purchaseId) {
        super("La compra " + purchaseId + " no existe");
    }
}

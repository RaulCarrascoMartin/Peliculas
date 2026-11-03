package shop.purchase.internal.application.usecase;

import shop.purchase.internal.application.dto.PurchaseView;
import shop.purchase.internal.application.mapper.PurchaseViewMapper;

import jakarta.transaction.Transactional;
import shop.purchase.internal.domain.service.PurchaseService;

public class CompletePurchase {

    private final PurchaseService purchases;

    public CompletePurchase(PurchaseService purchases) {
        this.purchases = purchases;
    }

    /**
     * Converts the cart into a purchase and clears it in the same transaction. The cart's optimistic lock fires at
     * flush or commit and {@code GlobalExceptionHandler} translates it to 409.
     */
    @Transactional
    public PurchaseView execute(Long userId) {
        return PurchaseViewMapper.toView(purchases.checkout(userId));
    }
}

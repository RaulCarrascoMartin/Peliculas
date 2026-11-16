package shop.purchase.internal.application.usecase;

import shop.purchase.internal.application.dto.PurchaseView;
import shop.purchase.internal.application.mapper.PurchaseViewMapper;

import shop.purchase.internal.domain.service.PurchaseService;

public class GetPurchase {

    private final PurchaseService purchases;

    public GetPurchase(PurchaseService purchases) {
        this.purchases = purchases;
    }

    /** Someone else's purchase is indistinguishable from a non-existent one: 404, never 403. */
    public PurchaseView execute(Long userId, Long purchaseId) {
        return PurchaseViewMapper.toView(purchases.detailOf(userId, purchaseId));
    }
}

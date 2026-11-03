package shop.purchase.internal.application.usecase;

import shop.purchase.internal.application.dto.PurchaseView;
import shop.purchase.internal.application.mapper.PurchaseViewMapper;

import shop.purchase.internal.domain.service.PurchaseService;

import java.util.List;

public class ListPurchases {

    private final PurchaseService purchases;

    public ListPurchases(PurchaseService purchases) {
        this.purchases = purchases;
    }

    public List<PurchaseView> execute(Long userId) {
        return purchases.historyOf(userId).stream().map(PurchaseViewMapper::toView).toList();
    }
}

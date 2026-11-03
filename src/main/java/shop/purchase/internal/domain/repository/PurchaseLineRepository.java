package shop.purchase.internal.domain.repository;

import shop.purchase.internal.domain.model.PurchaseLine;

import java.util.List;

public interface PurchaseLineRepository {

    PurchaseLine save(PurchaseLine line);

    List<PurchaseLine> findByPurchaseIdOrderById(Long purchaseId);
}

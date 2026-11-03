package shop.purchase.internal.domain.repository;

import shop.purchase.internal.domain.model.Purchase;

import java.util.List;
import java.util.Optional;

public interface PurchaseRepository {

    Purchase save(Purchase purchase);

    /** Own history, from most recent to oldest. */
    List<Purchase> findByUserIdOrderByPurchasedAtDesc(Long userId);

    /** Own detail: someone else's purchase is indistinguishable from a non-existent one. */
    Optional<Purchase> findByIdAndUserId(Long id, Long userId);
}

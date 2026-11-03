package shop.purchase.internal.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    /** Own history, from most recent to oldest. */
    List<Purchase> findByUserIdOrderByPurchasedAtDesc(Long userId);

    /** Own detail: someone else's purchase is indistinguishable from a non-existent one. */
    Optional<Purchase> findByIdAndUserId(Long id, Long userId);
}

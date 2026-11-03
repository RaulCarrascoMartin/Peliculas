package shop.purchase.internal.persistence.jpa;

import shop.purchase.internal.persistence.entity.PurchaseEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseJpaRepository extends JpaRepository<PurchaseEntity, Long> {

    /** Own history, from most recent to oldest. */
    List<PurchaseEntity> findByUserIdOrderByPurchasedAtDesc(Long userId);

    /** Own detail: someone else's purchase is indistinguishable from a non-existent one. */
    Optional<PurchaseEntity> findByIdAndUserId(Long id, Long userId);
}

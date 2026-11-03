package shop.purchase.internal.persistence.jpa;

import shop.purchase.internal.persistence.entity.PurchaseLineEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseLineJpaRepository extends JpaRepository<PurchaseLineEntity, Long> {

    List<PurchaseLineEntity> findByPurchaseIdOrderById(Long purchaseId);
}

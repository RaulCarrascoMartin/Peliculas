package shop.purchase.internal.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseLineRepository extends JpaRepository<PurchaseLine, Long> {

    List<PurchaseLine> findByPurchaseIdOrderById(Long purchaseId);
}

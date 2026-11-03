package shop.purchase.internal.usecase;

import shop.shared.error.ResourceNotFoundException;
import shop.purchase.internal.domain.Purchase;
import shop.purchase.internal.domain.PurchaseRepository;
import shop.purchase.internal.domain.PurchaseLineRepository;
import org.springframework.stereotype.Service;

@Service
public class GetPurchase {

    private final PurchaseRepository purchases;
    private final PurchaseLineRepository lines;

    public GetPurchase(PurchaseRepository purchases, PurchaseLineRepository lines) {
        this.purchases = purchases;
        this.lines = lines;
    }

    /** Someone else's purchase is indistinguishable from a non-existent one: 404, never 403. */
    public PurchaseView execute(Long userId, Long purchaseId) {
        Purchase purchase = purchases.findByIdAndUserId(purchaseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("La compra " + purchaseId + " no existe"));
        return PurchaseViewMapper.toView(purchase, lines.findByPurchaseIdOrderById(purchase.getId()));
    }
}

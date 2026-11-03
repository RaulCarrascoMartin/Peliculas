package shop.purchase.internal.usecase;

import shop.purchase.internal.domain.PurchaseRepository;
import shop.purchase.internal.domain.PurchaseLineRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListPurchases {

    private final PurchaseRepository purchases;
    private final PurchaseLineRepository lines;

    public ListPurchases(PurchaseRepository purchases, PurchaseLineRepository lines) {
        this.purchases = purchases;
        this.lines = lines;
    }

    public List<PurchaseView> execute(Long userId) {
        return purchases.findByUserIdOrderByPurchasedAtDesc(userId).stream()
                .map(purchase -> PurchaseViewMapper.toView(purchase, lines.findByPurchaseIdOrderById(purchase.getId())))
                .toList();
    }
}

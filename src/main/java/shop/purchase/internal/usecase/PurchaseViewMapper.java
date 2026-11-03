package shop.purchase.internal.usecase;

import shop.purchase.internal.domain.Purchase;
import shop.purchase.internal.domain.PurchaseLine;

import java.util.List;

/** Converts the entities into the records that leave the use cases: the model never crosses this boundary. */
final class PurchaseViewMapper {

    private PurchaseViewMapper() {
    }

    static PurchaseView toView(Purchase purchase, List<PurchaseLine> lines) {
        return new PurchaseView(purchase.getId(), purchase.getUserId(), purchase.getPurchasedAt(), purchase.getTotal(),
                purchase.getStatus().name(),
                lines.stream()
                        .map(l -> new PurchaseLineView(l.getMovieId(), l.getTitle(), l.getUnitPrice()))
                        .toList());
    }
}

package shop.purchase.internal.application.mapper;

import shop.purchase.internal.application.dto.PurchaseLineView;
import shop.purchase.internal.application.dto.PurchaseView;

import shop.purchase.internal.domain.service.PurchaseService.PurchaseDetail;

/** Converts the domain result into the records that leave the use cases: the model never crosses this boundary. */
public final class PurchaseViewMapper {

    private PurchaseViewMapper() {
    }

    public static PurchaseView toView(PurchaseDetail detail) {
        var purchase = detail.purchase();
        return new PurchaseView(purchase.getId(), purchase.getUserId(), purchase.getPurchasedAt(), purchase.getTotal(),
                purchase.getStatus().name(),
                detail.lines().stream()
                        .map(l -> new PurchaseLineView(l.getMovieId(), l.getTitle(), l.getUnitPrice()))
                        .toList());
    }
}

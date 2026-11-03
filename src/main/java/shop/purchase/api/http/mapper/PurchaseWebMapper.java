package shop.purchase.api.http.mapper;

import shop.purchase.api.http.dto.Purchase;
import shop.purchase.internal.application.dto.PurchaseLineView;
import shop.purchase.internal.application.dto.PurchaseView;

import java.time.ZoneId;
import java.util.List;

/** Converts the use case views into the DTO of the HTTP contract. */
public final class PurchaseWebMapper {

    private PurchaseWebMapper() {
    }

    /** The view holds a {@code LocalDateTime} (server time); the contract publishes {@code date-time} with an offset. */
    public static Purchase toDto(PurchaseView view) {
        List<shop.purchase.api.http.dto.PurchaseLine> lines = view.lines().stream()
                .map(PurchaseWebMapper::toDto)
                .toList();
        return new Purchase(view.id(), view.purchasedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                Purchase.EstadoEnum.valueOf(view.status()), lines, view.total());
    }

    public static shop.purchase.api.http.dto.PurchaseLine toDto(PurchaseLineView line) {
        return new shop.purchase.api.http.dto.PurchaseLine(line.movieId(), line.title(), line.unitPrice());
    }
}

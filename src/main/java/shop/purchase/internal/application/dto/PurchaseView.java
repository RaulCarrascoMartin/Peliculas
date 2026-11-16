package shop.purchase.internal.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** A purchase with its frozen lines, as returned by the purchase use cases. */
public record PurchaseView(
        Long id,
        Long userId,
        LocalDateTime purchasedAt,
        BigDecimal total,
        String status,
        List<PurchaseLineView> lines) {
}

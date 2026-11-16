package shop.purchase.internal.domain.dto;

import java.math.BigDecimal;

/** Frozen line of a purchase: title and price as they were at purchase time, not the current catalog ones. */
public record PurchaseLineDto(
        Long id,
        Long purchaseId,
        Long movieId,
        String title,
        BigDecimal unitPrice) {
}

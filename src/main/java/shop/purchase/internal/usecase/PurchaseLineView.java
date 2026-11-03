package shop.purchase.internal.usecase;

import java.math.BigDecimal;

/** Frozen line of a purchase: title and price as they were at purchase time. */
public record PurchaseLineView(
        Long movieId,
        String title,
        BigDecimal unitPrice) {
}

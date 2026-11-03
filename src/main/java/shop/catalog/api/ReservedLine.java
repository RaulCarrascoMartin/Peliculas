package shop.catalog.api;

import java.math.BigDecimal;

/** Reserved line with the title and unit price captured at reservation time. */
public record ReservedLine(
        Long productId,
        String title,
        BigDecimal unitPrice,
        int quantity) {
}

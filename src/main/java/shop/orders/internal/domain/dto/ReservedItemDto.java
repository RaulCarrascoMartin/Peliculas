package shop.orders.internal.domain.dto;

import java.math.BigDecimal;

/** Reserved product with the title and unit price captured at reservation time. */
public record ReservedItemDto(
        Long productId,
        String title,
        BigDecimal unitPrice,
        int quantity) {
}

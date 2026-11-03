package shop.catalog.api.facade.dto;

import java.math.BigDecimal;

/** Reserved line with the title and unit price captured at reservation time. */
public record ReservedLineDto(
        Long productId,
        String title,
        BigDecimal unitPrice,
        int quantity) {
}

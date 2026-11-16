package shop.orders.internal.domain.dto;

import java.math.BigDecimal;

/** Order line; {@code unitPrice} is the price captured at reservation, not the current catalog one. */
public record OrderLineDto(
        Long productId,
        String title,
        BigDecimal unitPrice,
        int quantity) {
}

package shop.orders.internal.application.dto;

import java.math.BigDecimal;

public record OrderLineView(
        Long productId,
        String title,
        BigDecimal unitPrice,
        int quantity) {
}

package shop.orders.internal.usecase;

import java.math.BigDecimal;

public record OrderLineView(
        Long productId,
        String title,
        BigDecimal unitPrice,
        int quantity) {
}

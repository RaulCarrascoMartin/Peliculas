package shop.orders.internal.domain.dto;

import java.util.List;

/** Input of the order creation. {@code userId} is the authenticated user. */
public record CreateOrderDto(
        Long userId,
        List<StockItemDto> lines) {
}

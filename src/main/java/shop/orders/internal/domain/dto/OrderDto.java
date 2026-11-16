package shop.orders.internal.domain.dto;

import shop.orders.internal.domain.model.OrderStatus;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Order with its lines as it travels between layers: controller, service and repository. */
public record OrderDto(
        Long id,
        Long userId,
        OrderStatus status,
        Instant createdAt,
        BigDecimal total,
        List<OrderLineDto> lines) {
}

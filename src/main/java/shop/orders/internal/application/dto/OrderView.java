package shop.orders.internal.application.dto;

import shop.orders.internal.domain.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Order with its lines as returned by the use cases: the model does not leave {@code usecase}. */
public record OrderView(
        Long id,
        Long userId,
        String status,
        Instant createdAt,
        BigDecimal total,
        List<OrderLineView> lines) {

    public static OrderView of(Order order) {
        return new OrderView(order.getId(), order.getUserId(), order.getStatus().name(), order.getCreatedAt(),
                order.getTotal(),
                order.getLines().stream()
                        .map(l -> new OrderLineView(l.getProductId(), l.getTitle(), l.getUnitPrice(), l.getQuantity()))
                        .toList());
    }
}

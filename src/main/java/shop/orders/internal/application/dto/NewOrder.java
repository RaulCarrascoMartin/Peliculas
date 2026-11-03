package shop.orders.internal.application.dto;

import java.util.List;

/** Input of the create-order use case. {@code userId} is provisional until milestone 08. */
public record NewOrder(
        Long userId,
        List<NewOrderLine> lines) {
}

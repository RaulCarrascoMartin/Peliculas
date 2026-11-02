package shop.orders.internal.usecase;

import java.util.List;

/** Entrada del caso de uso de crear pedido. {@code userId} es provisional hasta el hito 08. */
public record NewOrder(
        Long userId,
        List<NewOrderLine> lines) {
}

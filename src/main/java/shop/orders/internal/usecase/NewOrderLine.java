package shop.orders.internal.usecase;

/** Line requested by the customer: product and quantity, without price (the reservation captures it). */
public record NewOrderLine(
        Long productId,
        int quantity) {
}

package shop.orders.internal.domain.dto;

/** Quantity of a product to order, reserve or release; without price (the reservation captures it). */
public record StockItemDto(
        Long productId,
        int quantity) {
}

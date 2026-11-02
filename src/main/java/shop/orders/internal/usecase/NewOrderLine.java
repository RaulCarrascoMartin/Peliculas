package shop.orders.internal.usecase;

/** Línea pedida por el cliente: producto y cantidad, sin precio (lo captura la reserva). */
public record NewOrderLine(
        Long productId,
        int quantity) {
}

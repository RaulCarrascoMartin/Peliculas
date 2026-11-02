package shop.catalog.api;

/** Cantidad de un producto que se pide reservar o liberar. */
public record StockLine(
        Long productId,
        int quantity) {
}

package shop.catalog.api;

/** Quantity of a product requested to be reserved or released. */
public record StockLine(
        Long productId,
        int quantity) {
}

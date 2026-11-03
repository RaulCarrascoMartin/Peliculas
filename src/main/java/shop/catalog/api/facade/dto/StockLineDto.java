package shop.catalog.api.facade.dto;

/** Quantity of a product requested to be reserved or released. */
public record StockLineDto(
        Long productId,
        int quantity) {
}

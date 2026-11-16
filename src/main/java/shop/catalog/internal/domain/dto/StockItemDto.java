package shop.catalog.internal.domain.dto;

/** Quantity of a movie to reserve or release. */
public record StockItemDto(
        Long movieId,
        int quantity) {
}

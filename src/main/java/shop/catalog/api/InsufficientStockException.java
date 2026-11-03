package shop.catalog.api;

/** There are not enough units of a product to fulfil the reservation. */
public class InsufficientStockException extends RuntimeException {

    private final Long productId;

    public InsufficientStockException(Long productId) {
        super("No hay stock suficiente del producto " + productId);
        this.productId = productId;
    }

    public Long productId() {
        return productId;
    }
}

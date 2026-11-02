package shop.catalog.api;

/** No hay unidades suficientes de un producto para atender la reserva. */
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

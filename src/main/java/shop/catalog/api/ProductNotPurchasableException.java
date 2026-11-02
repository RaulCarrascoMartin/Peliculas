package shop.catalog.api;

/** El producto existe pero no tiene precio, así que no se puede reservar para una compra. */
public class ProductNotPurchasableException extends RuntimeException {

    private final Long productId;

    public ProductNotPurchasableException(Long productId) {
        super("El producto " + productId + " no tiene precio y no es comprable");
        this.productId = productId;
    }

    public Long productId() {
        return productId;
    }
}

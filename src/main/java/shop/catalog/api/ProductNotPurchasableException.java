package shop.catalog.api;

/** The product exists but has no price, so it cannot be reserved for a purchase. */
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

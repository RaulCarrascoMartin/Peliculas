package shop.catalog.api.facade.exception;

/** The requested product does not exist in the catalog. */
public class UnknownProductException extends RuntimeException {

    private final Long productId;

    public UnknownProductException(Long productId) {
        super("El producto " + productId + " no existe");
        this.productId = productId;
    }

    public Long productId() {
        return productId;
    }
}

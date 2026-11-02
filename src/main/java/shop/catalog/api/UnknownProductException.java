package shop.catalog.api;

/** El producto pedido no existe en el catálogo. */
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

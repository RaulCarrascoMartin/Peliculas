package shop.shared.error;

/** Business validation rule violated; translated to 400. */
public class BusinessValidationException extends RuntimeException {
    public BusinessValidationException(String message) {
        super(message);
    }
}

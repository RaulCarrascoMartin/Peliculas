package shop.shared.error;

/** Login failed: unknown email or wrong password. The handler translates it to 401. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}

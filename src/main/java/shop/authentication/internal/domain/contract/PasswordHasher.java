package shop.authentication.internal.domain.contract;

/** Password hashing as the domain needs it; the implementation lives in {@code internal.infrastructure}. */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}

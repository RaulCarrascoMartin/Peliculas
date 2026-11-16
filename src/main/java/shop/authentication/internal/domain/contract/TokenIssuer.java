package shop.authentication.internal.domain.contract;

/** Issues the access token of a user; the implementation lives in {@code internal.infrastructure}. */
public interface TokenIssuer {

    String issue(Long userId, String role);
}

package shop.authentication.internal.domain.dto;

/** Credentials to check: the email and the password as typed. */
public record LoginDto(
        String email,
        String password) {
}

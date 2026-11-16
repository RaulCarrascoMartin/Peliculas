package shop.authentication.internal.domain.dto;

/** Input of the registration: the email and the password as typed (the service hashes it). */
public record RegisterUserDto(
        String email,
        String password) {
}

package shop.authentication.internal.usecase;

/** Registered user, without password: what the register use case returns. */
public record UserView(
        Long id,
        String email,
        String role) {
}

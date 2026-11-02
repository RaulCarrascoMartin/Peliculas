package shop.autenticacion.internal.usecase;

/** Usuario registrado, sin contraseña: lo que devuelve el caso de uso de registro. */
public record UsuarioVista(
        Long id,
        String email,
        String rol) {
}

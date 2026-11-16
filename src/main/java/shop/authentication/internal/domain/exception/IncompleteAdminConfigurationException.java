package shop.authentication.internal.domain.exception;

/**
 * Only one of the two values of the initial admin (email, password) is configured. It is a startup failure of the
 * deployment, never the answer to an HTTP request, so it does not extend an HTTP category.
 */
public class IncompleteAdminConfigurationException extends IllegalStateException {

    public IncompleteAdminConfigurationException() {
        super("peliculas.admin.email and peliculas.admin.contrasena must be configured together");
    }
}

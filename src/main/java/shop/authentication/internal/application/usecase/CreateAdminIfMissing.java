package shop.authentication.internal.application.usecase;

import shop.authentication.internal.domain.service.AuthService;

/** Startup use case: creates the initial admin from the configuration, if any. */
public class CreateAdminIfMissing {

    private final AuthService auth;

    public CreateAdminIfMissing(AuthService auth) {
        this.auth = auth;
    }

    /** Never modifies an existing user; both values blank means "not configured". */
    public void execute(String email, String password) {
        auth.ensureInitialAdmin(email, password);
    }
}

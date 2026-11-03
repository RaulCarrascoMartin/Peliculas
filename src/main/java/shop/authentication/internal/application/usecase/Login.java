package shop.authentication.internal.application.usecase;

import shop.authentication.internal.domain.service.AuthService;

public class Login {

    private final AuthService auth;

    public Login(AuthService auth) {
        this.auth = auth;
    }

    /** Returns the user's JWT; the same error is used for unknown email and wrong password. */
    public String execute(String email, String password) {
        return auth.login(email, password);
    }
}

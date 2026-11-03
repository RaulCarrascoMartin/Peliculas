package shop.authentication.internal.application.usecase;

import shop.authentication.internal.application.dto.UserView;

import shop.authentication.internal.domain.service.AuthService;
import shop.authentication.internal.domain.model.User;

public class RegisterUser {

    private final AuthService auth;

    public RegisterUser(AuthService auth) {
        this.auth = auth;
    }

    public UserView execute(String email, String password) {
        User saved = auth.register(email, password);
        return new UserView(saved.getId(), saved.getEmail(), saved.getRole().name());
    }
}

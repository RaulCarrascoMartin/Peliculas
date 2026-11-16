package shop.authentication.api.http.controller;

import org.springframework.web.bind.annotation.RestController;
import shop.authentication.api.http.AuthHttpApi;
import shop.authentication.api.http.dto.LoginRequest;
import shop.authentication.api.http.dto.RegisterRequest;
import shop.authentication.api.http.dto.TokenResponse;
import shop.authentication.api.http.mapper.AuthWebMapper;
import shop.authentication.internal.domain.service.AuthService;

/** HTTP entry of authentication: implements the hand-written contract {@link AuthHttpApi} and delegates. */
@RestController
public class AuthController implements AuthHttpApi {

    private final AuthService authService;
    private final AuthWebMapper mapper;

    public AuthController(AuthService authService, AuthWebMapper mapper) {
        this.authService = authService;
        this.mapper = mapper;
    }

    @Override
    public void register(RegisterRequest request) {
        authService.register(mapper.toDto(request));
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        return mapper.toResponse(authService.login(mapper.toDto(request)));
    }
}

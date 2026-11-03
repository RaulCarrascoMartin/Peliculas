package shop.authentication.internal.web;

import jakarta.validation.Valid;
import shop.authentication.internal.usecase.Login;
import shop.authentication.internal.usecase.RegisterUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUser registerUser;
    private final Login login;

    public AuthController(RegisterUser registerUser, Login login) {
        this.registerUser = registerUser;
        this.login = login;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        registerUser.execute(request.email(), request.password());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return new TokenResponse(login.execute(request.email(), request.password()));
    }
}

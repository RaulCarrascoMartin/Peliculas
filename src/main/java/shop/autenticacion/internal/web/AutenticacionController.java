package shop.autenticacion.internal.web;

import jakarta.validation.Valid;
import shop.autenticacion.internal.usecase.IniciarSesion;
import shop.autenticacion.internal.usecase.RegistrarUsuario;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacionController {

    private final RegistrarUsuario registrar;
    private final IniciarSesion iniciarSesion;

    public AutenticacionController(RegistrarUsuario registrar, IniciarSesion iniciarSesion) {
        this.registrar = registrar;
        this.iniciarSesion = iniciarSesion;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public void registrar(@Valid @RequestBody RegistroRequest peticion) {
        registrar.ejecutar(peticion.email(), peticion.contrasena());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest peticion) {
        return new TokenResponse(iniciarSesion.ejecutar(peticion.email(), peticion.contrasena()));
    }
}

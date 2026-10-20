package org.example.autenticacion.web;

import jakarta.validation.Valid;
import org.example.autenticacion.application.AutenticacionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacionController {

    private final AutenticacionService servicio;

    public AutenticacionController(AutenticacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public void registrar(@Valid @RequestBody RegistroRequest peticion) {
        servicio.registrar(peticion.email(), peticion.contrasena());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest peticion) {
        return new TokenResponse(servicio.login(peticion.email(), peticion.contrasena()));
    }
}

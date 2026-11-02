package org.example.autenticacion.internal.usecase;

import org.example.autenticacion.internal.domain.UsuarioRepository;
import org.example.compartido.JwtTokens;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class IniciarSesion {

    private static final String CREDENCIALES_INCORRECTAS = "Credenciales incorrectas";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final JwtTokens tokens;

    public IniciarSesion(UsuarioRepository usuarios, PasswordEncoder codificador, JwtTokens tokens) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.tokens = tokens;
    }

    /** Devuelve el JWT del usuario; el mismo error sirve para email inexistente y contraseña errónea. */
    public String ejecutar(String email, String contrasena) {
        return usuarios.findByEmail(email)
                .filter(u -> codificador.matches(contrasena, u.getContrasena()))
                .map(u -> tokens.emitir(u.getId(), u.getRol().name()))
                .orElseThrow(() -> new BadCredentialsException(CREDENCIALES_INCORRECTAS));
    }
}

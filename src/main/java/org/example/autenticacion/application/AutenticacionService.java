package org.example.autenticacion.application;

import org.example.autenticacion.domain.Rol;
import org.example.autenticacion.domain.Usuario;
import org.example.autenticacion.domain.UsuarioRepository;
import org.example.compartido.ConflictoException;
import org.example.compartido.JwtTokens;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacionService {

    private static final String CREDENCIALES_INCORRECTAS = "Credenciales incorrectas";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final JwtTokens tokens;

    public AutenticacionService(UsuarioRepository usuarios, PasswordEncoder codificador, JwtTokens tokens) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.tokens = tokens;
    }

    public Usuario registrar(String email, String contrasena) {
        if (usuarios.existsByEmail(email)) {
            throw new ConflictoException("El email ya está registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setContrasena(codificador.encode(contrasena));
        usuario.setRol(Rol.USUARIO);
        return usuarios.save(usuario);
    }

    /** Crea un administrador si no hay ya un usuario con ese email; nunca modifica uno existente. */
    public void crearAdministradorSiNoExiste(String email, String contrasena) {
        if (usuarios.existsByEmail(email)) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setEmail(email);
        admin.setContrasena(codificador.encode(contrasena));
        admin.setRol(Rol.ADMIN);
        usuarios.save(admin);
    }

    /** Devuelve el JWT del usuario; el mismo error sirve para email inexistente y contraseña errónea. */
    public String login(String email, String contrasena) {
        Usuario usuario = usuarios.findByEmail(email)
                .filter(u -> codificador.matches(contrasena, u.getContrasena()))
                .orElseThrow(() -> new BadCredentialsException(CREDENCIALES_INCORRECTAS));
        return tokens.emitir(usuario.getId(), usuario.getRol().name());
    }
}

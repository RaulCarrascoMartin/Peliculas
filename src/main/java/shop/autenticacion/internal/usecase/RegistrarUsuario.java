package shop.autenticacion.internal.usecase;

import shop.autenticacion.internal.domain.Rol;
import shop.autenticacion.internal.domain.Usuario;
import shop.autenticacion.internal.domain.UsuarioRepository;
import shop.compartido.error.ConflictoException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistrarUsuario {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;

    public RegistrarUsuario(UsuarioRepository usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    public UsuarioVista ejecutar(String email, String contrasena) {
        if (usuarios.existsByEmail(email)) {
            throw new ConflictoException("El email ya está registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setContrasena(codificador.encode(contrasena));
        usuario.setRol(Rol.USUARIO);
        Usuario guardado = usuarios.save(usuario);
        return new UsuarioVista(guardado.getId(), guardado.getEmail(), guardado.getRol().name());
    }
}

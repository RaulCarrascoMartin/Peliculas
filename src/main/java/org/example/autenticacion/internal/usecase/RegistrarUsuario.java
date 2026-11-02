package org.example.autenticacion.internal.usecase;

import org.example.autenticacion.internal.domain.Rol;
import org.example.autenticacion.internal.domain.Usuario;
import org.example.autenticacion.internal.domain.UsuarioRepository;
import org.example.compartido.ConflictoException;
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

    public Usuario ejecutar(String email, String contrasena) {
        if (usuarios.existsByEmail(email)) {
            throw new ConflictoException("El email ya está registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setContrasena(codificador.encode(contrasena));
        usuario.setRol(Rol.USUARIO);
        return usuarios.save(usuario);
    }
}

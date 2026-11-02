package org.example.autenticacion.internal.usecase;

import org.example.autenticacion.internal.domain.Rol;
import org.example.autenticacion.internal.domain.Usuario;
import org.example.autenticacion.internal.domain.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CrearAdministradorSiNoExiste {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;

    public CrearAdministradorSiNoExiste(UsuarioRepository usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    /** Crea un administrador si no hay ya un usuario con ese email; nunca modifica uno existente. */
    public void ejecutar(String email, String contrasena) {
        if (usuarios.existsByEmail(email)) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setEmail(email);
        admin.setContrasena(codificador.encode(contrasena));
        admin.setRol(Rol.ADMIN);
        usuarios.save(admin);
    }
}

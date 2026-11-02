package org.example.autenticacion.internal.usecase;

import org.example.autenticacion.internal.domain.Usuario;
import org.example.autenticacion.internal.domain.UsuarioRepository;
import org.example.compartido.RepositorioEnMemoria;

import java.util.Optional;

public class UsuarioRepositoryFalso extends RepositorioEnMemoria<Usuario> implements UsuarioRepository {

    @Override
    protected Long idDe(Usuario usuario) {
        return usuario.getId();
    }

    @Override
    protected void asignarId(Usuario usuario, Long id) {
        usuario.setId(id);
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        return entidades.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }
}

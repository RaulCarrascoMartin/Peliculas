package org.example.compartido;

import java.util.Set;

/** Doble de prueba de {@link UsuarioActual}: no toca el SecurityContextHolder. */
public class UsuarioActualFalso extends UsuarioActual {

    private final Long id;
    private final Set<String> roles;

    public UsuarioActualFalso(Long id, String... roles) {
        this.id = id;
        this.roles = Set.of(roles);
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Set<String> roles() {
        return roles;
    }
}

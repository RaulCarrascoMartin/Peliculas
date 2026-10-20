package org.example.compartido;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Único punto de lectura de {@link SecurityContextHolder}. El principal de la
 * autenticación es el id del usuario ({@link Long}) y las autoridades llevan el prefijo {@code ROLE_}.
 */
@Component
public class UsuarioActual {

    private static final String PREFIJO_ROL = "ROLE_";
    private static final String ROL_ADMIN = "ADMIN";

    public Long id() {
        return (Long) autenticacion().getPrincipal();
    }

    public Set<String> roles() {
        return autenticacion().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(UsuarioActual::sinPrefijo)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean esAdmin() {
        return roles().contains(ROL_ADMIN);
    }

    private static String sinPrefijo(String autoridad) {
        return autoridad.startsWith(PREFIJO_ROL) ? autoridad.substring(PREFIJO_ROL.length()) : autoridad;
    }

    private static Authentication autenticacion() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || autenticacion instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("No hay un usuario autenticado");
        }
        return autenticacion;
    }
}

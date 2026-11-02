package shop.compartido.seguridad;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioActualTest {

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(Long id, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(id, null, AuthorityUtils.createAuthorityList(authorities)));
    }

    @Test
    void devuelveElIdDelUsuarioAutenticado() {
        autenticar(5L, "ROLE_USUARIO");

        assertEquals(5L, new UsuarioActual().id());
    }

    @Test
    void devuelveLosRolesSinElPrefijoROLE() {
        autenticar(5L, "ROLE_ADMIN");

        assertEquals(Set.of("ADMIN"), new UsuarioActual().roles());
    }

    @Test
    void esAdminSoloConRolAdmin() {
        autenticar(1L, "ROLE_ADMIN");
        assertTrue(new UsuarioActual().esAdmin());

        autenticar(2L, "ROLE_USUARIO");
        assertFalse(new UsuarioActual().esAdmin());
    }

    @Test
    void sinAutenticacionLanzaExcepcionDeAutenticacion() {
        assertThrows(AuthenticationException.class, () -> new UsuarioActual().id());
    }

    @Test
    void usuarioAnonimoNoCuentaComoAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "clave", "anonimo", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThrows(AuthenticationException.class, () -> new UsuarioActual().id());
    }

    @Test
    void elDobleDePruebaNoUsaElSecurityContext() {
        var falso = new UsuarioActualFalso(9L, "ADMIN");

        assertEquals(9L, falso.id());
        assertEquals(Set.of("ADMIN"), falso.roles());
        assertTrue(falso.esAdmin());
    }
}

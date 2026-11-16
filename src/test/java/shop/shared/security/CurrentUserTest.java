package shop.shared.security;

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

class CurrentUserTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long id, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(id, null, AuthorityUtils.createAuthorityList(authorities)));
    }

    @Test
    void returnsIdOfAuthenticatedUser() {
        authenticate(5L, "ROLE_USUARIO");

        assertEquals(5L, new CurrentUser().id());
    }

    @Test
    void returnsRolesWithoutROLEPrefix() {
        authenticate(5L, "ROLE_ADMIN");

        assertEquals(Set.of("ADMIN"), new CurrentUser().roles());
    }

    @Test
    void isAdminOnlyWithAdminRole() {
        authenticate(1L, "ROLE_ADMIN");
        assertTrue(new CurrentUser().isAdmin());

        authenticate(2L, "ROLE_USUARIO");
        assertFalse(new CurrentUser().isAdmin());
    }

    @Test
    void withoutAuthenticationThrowsAuthenticationException() {
        assertThrows(AuthenticationException.class, () -> new CurrentUser().id());
    }

    @Test
    void anonymousUserDoesNotCountAsAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThrows(AuthenticationException.class, () -> new CurrentUser().id());
    }

    @Test
    void testDoubleDoesNotUseSecurityContext() {
        var fake = new CurrentUserFake(9L, "ADMIN");

        assertEquals(9L, fake.id());
        assertEquals(Set.of("ADMIN"), fake.roles());
        assertTrue(fake.isAdmin());
    }
}

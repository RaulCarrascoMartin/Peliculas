package shop.shared.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single point of reading {@link SecurityContextHolder}. The authentication principal
 * is the user id ({@link Long}) and the authorities carry the {@code ROLE_} prefix.
 */
@Component
public class CurrentUser {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ADMIN_ROLE = "ADMIN";

    public Long id() {
        return (Long) authentication().getPrincipal();
    }

    public Set<String> roles() {
        return authentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(CurrentUser::withoutPrefix)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin() {
        return roles().contains(ADMIN_ROLE);
    }

    private static String withoutPrefix(String authority) {
        return authority.startsWith(ROLE_PREFIX) ? authority.substring(ROLE_PREFIX.length()) : authority;
    }

    private static Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("There is no authenticated user");
        }
        return authentication;
    }
}

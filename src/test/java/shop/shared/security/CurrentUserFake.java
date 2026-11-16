package shop.shared.security;

import java.util.Set;

/** Test double of {@link CurrentUser}: it does not touch the SecurityContextHolder. */
public class CurrentUserFake extends CurrentUser {

    private final Long id;
    private final Set<String> roles;

    public CurrentUserFake(Long id, String... roles) {
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

package shop.authentication.internal.domain.dto;

import shop.authentication.internal.domain.model.Role;


/**
 * User as it travels between the service and the repository. {@code passwordHash} is filled only on that side: what
 * the service hands to the controller or to the token provider carries it as {@code null}.
 */
public record UserDto(
        Long id,
        String email,
        String passwordHash,
        Role role) {
}

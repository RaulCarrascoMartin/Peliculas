package shop.authentication.internal.domain.contract;

import shop.authentication.internal.domain.dto.UserDto;

/**
 * Generates the access token of a user; the implementation lives in {@code internal.infrastructure}. Incoming tokens
 * are validated by the shared security filter, not by this module, so there is no {@code validate} here.
 */
public interface TokenProvider {

    /** Token that identifies the user (id) and carries its role. */
    String generate(UserDto user);
}

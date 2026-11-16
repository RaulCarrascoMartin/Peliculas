package shop.authentication.internal.domain.repository;

import shop.authentication.internal.domain.dto.UserDto;
import shop.authentication.internal.domain.exception.EmailAlreadyRegisteredException;

import java.util.Optional;

/** User persistence; the implementation uses JDBC and lives in {@code internal.persistence}. */
public interface UserRepository {

    /**
     * Inserts the user and returns it with the assigned id.
     *
     * @throws EmailAlreadyRegisteredException if another user already has that email
     */
    UserDto save(UserDto user);

    Optional<UserDto> findByEmail(String email);

    boolean existsByEmail(String email);
}

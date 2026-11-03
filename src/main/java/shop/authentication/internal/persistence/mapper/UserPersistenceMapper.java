package shop.authentication.internal.persistence.mapper;

import shop.authentication.internal.persistence.entity.UserEntity;

import shop.authentication.internal.domain.model.Role;
import shop.authentication.internal.domain.model.User;

/** Converts between the framework-free domain model and the JPA entities. */
public final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    public static User toDomain(UserEntity e) {
        var d = new User();
        d.setId(e.getId());
        d.setEmail(e.getEmail());
        d.setPassword(e.getPassword());
        d.setRole(e.getRole());
        return d;
    }

    public static UserEntity toEntity(User d) {
        var e = new UserEntity();
        e.setId(d.getId());
        e.setEmail(d.getEmail());
        e.setPassword(d.getPassword());
        e.setRole(d.getRole());
        return e;
    }
}

package shop.authentication.internal.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import shop.authentication.internal.domain.dto.UserDto;
import shop.authentication.internal.domain.model.User;

/** DTO ↔ model of the authentication domain. Only transforms data: the rules live in the service. */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "password", source = "passwordHash")
    User toModel(UserDto dto);

    /** The whole user, hash included: only for the repository. */
    @Mapping(target = "passwordHash", source = "password")
    UserDto toDto(User user);

    /** The user without its password hash: what may leave the service. */
    @Mapping(target = "passwordHash", ignore = true)
    UserDto toPublicDto(User user);
}

package shop.authentication.internal.domain.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.authentication.internal.domain.dto.UserDto;
import shop.authentication.internal.domain.model.Role;
import shop.authentication.internal.domain.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void dtoSurvivesTheRoundTripThroughTheModel() {
        var dto = new UserDto(5L, "ana@correo.com", "bcrypt-hash", Role.ADMIN);

        User model = mapper.toModel(dto);

        assertEquals(5L, model.getId());
        assertEquals("ana@correo.com", model.getEmail());
        assertEquals("bcrypt-hash", model.getPassword());
        assertEquals(Role.ADMIN, model.getRole());
        assertEquals(dto, mapper.toDto(model));
    }

    @Test
    void publicDtoKeepsEverythingButThePasswordHash() {
        User model = mapper.toModel(new UserDto(5L, "ana@correo.com", "bcrypt-hash", Role.USUARIO));

        assertEquals(new UserDto(5L, "ana@correo.com", null, Role.USUARIO), mapper.toPublicDto(model));
    }
}

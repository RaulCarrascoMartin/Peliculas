package shop.authentication.api.http.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.authentication.api.http.dto.LoginRequest;
import shop.authentication.api.http.dto.RegisterRequest;
import shop.authentication.api.http.dto.TokenResponse;
import shop.authentication.internal.domain.dto.LoginDto;
import shop.authentication.internal.domain.dto.RegisterUserDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthWebMapperTest {

    private final AuthWebMapper mapper = Mappers.getMapper(AuthWebMapper.class);

    @Test
    void registerRequestBecomesTheDtoOfTheService() {
        assertEquals(new RegisterUserDto("ana@correo.com", "Secreta123"),
                mapper.toDto(new RegisterRequest("ana@correo.com", "Secreta123")));
    }

    @Test
    void loginRequestBecomesTheDtoOfTheService() {
        assertEquals(new LoginDto("ana@correo.com", "Secreta123"),
                mapper.toDto(new LoginRequest("ana@correo.com", "Secreta123")));
    }

    @Test
    void tokenBecomesTheResponseOfTheContract() {
        assertEquals(new TokenResponse("the.jwt.token"), mapper.toResponse("the.jwt.token"));
    }
}

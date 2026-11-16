package shop.authentication.internal.infrastructure;

import org.junit.jupiter.api.Test;
import shop.authentication.internal.domain.dto.UserDto;
import shop.authentication.internal.domain.model.Role;
import shop.shared.security.JwtTokens;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenProviderTest {

    private final JwtTokens jwt = new JwtTokens(
            "secreto-de-pruebas-con-al-menos-32-bytes-de-longitud", Duration.ofHours(1));
    private final JwtTokenProvider provider = new JwtTokenProvider(jwt);

    @Test
    void tokenCarriesTheUserIdAsSubjectAndItsRole() {
        var data = jwt.parseToken(provider.generate(new UserDto(5L, "root@correo.com", null, Role.ADMIN)));

        assertEquals(5L, data.id());
        assertEquals("ADMIN", data.role());
    }

    @Test
    void eachRoleTravelsWithItsName() {
        var data = jwt.parseToken(provider.generate(new UserDto(9L, "ana@correo.com", null, Role.USUARIO)));

        assertEquals(9L, data.id());
        assertEquals("USUARIO", data.role());
    }
}

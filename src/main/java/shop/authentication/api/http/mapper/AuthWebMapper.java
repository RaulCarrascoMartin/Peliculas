package shop.authentication.api.http.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import shop.authentication.api.http.dto.LoginRequest;
import shop.authentication.api.http.dto.RegisterRequest;
import shop.authentication.api.http.dto.TokenResponse;
import shop.authentication.internal.domain.dto.LoginDto;
import shop.authentication.internal.domain.dto.RegisterUserDto;

/** DTO of the HTTP contract ↔ DTO of the authentication domain. */
@Mapper(componentModel = "spring")
public interface AuthWebMapper {

    RegisterUserDto toDto(RegisterRequest request);

    LoginDto toDto(LoginRequest request);

    @Mapping(target = "token", source = "token")
    TokenResponse toResponse(String token);
}

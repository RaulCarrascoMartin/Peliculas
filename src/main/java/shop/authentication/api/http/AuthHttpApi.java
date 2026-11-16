package shop.authentication.api.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import shop.authentication.api.http.dto.LoginRequest;
import shop.authentication.api.http.dto.RegisterRequest;
import shop.authentication.api.http.dto.TokenResponse;

/** HTTP contract of authentication: the single source of truth of its routes, bodies and status codes. */
@Tag(name = "Auth", description = "Registro e inicio de sesión")
public interface AuthHttpApi {

    String PATH_REGISTER = "/api/auth/registro";
    String PATH_LOGIN = "/api/auth/login";

    @Operation(summary = "Registra una cuenta con rol de usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada (sin cuerpo)"),
            @ApiResponse(responseCode = "400", description = "Email no válido o contraseña fuera de 8-72 caracteres",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "El email ya está registrado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(PATH_REGISTER)
    @ResponseStatus(HttpStatus.CREATED)
    void register(@Valid @RequestBody RegisterRequest request);

    @Operation(summary = "Comprueba las credenciales y devuelve el token de acceso")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token JWT con el id y el rol del usuario"),
            @ApiResponse(responseCode = "400", description = "Falta el email o la contraseña",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401",
                    description = "Credenciales incorrectas (mismo error si el email no existe o la contraseña falla)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(PATH_LOGIN)
    @ResponseStatus(HttpStatus.OK)
    TokenResponse login(@Valid @RequestBody LoginRequest request);
}

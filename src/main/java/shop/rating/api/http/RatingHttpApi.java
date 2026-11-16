package shop.rating.api.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import shop.rating.api.http.dto.RatingRequest;
import shop.rating.api.http.dto.RatingResponse;

/** HTTP contract of ratings: the single source of truth of its routes, validation and status codes. */
@Tag(name = "Valoraciones")
public interface RatingHttpApi {

    @Operation(summary = "Valora una película (o cambia la valoración que el usuario ya tenía) y recalcula su media")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La valoración del usuario tal como queda guardada"),
            @ApiResponse(responseCode = "400", description = "Puntuación ausente o fuera del rango 1-5"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido o caducado"),
            @ApiResponse(responseCode = "404", description = "La película no existe"),
            @ApiResponse(responseCode = "409", description = "Dos valoraciones simultáneas del mismo usuario")
    })
    @PutMapping("/api/peliculas/{movieId}/valoracion")
    @ResponseStatus(HttpStatus.OK)
    RatingResponse rateMovie(@PathVariable("movieId") Long movieId, @Valid @RequestBody RatingRequest request);
}

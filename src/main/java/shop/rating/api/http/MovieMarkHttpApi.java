package shop.rating.api.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import shop.rating.api.http.dto.MarkedMovieResponse;

import java.util.List;

/** HTTP contract of the favourite / watched lists: the single source of truth of its routes and status codes. */
@Tag(name = "Marcas de película")
public interface MovieMarkHttpApi {

    @Operation(summary = "Marca una película como favorita; si ya lo era, no hace nada")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "La película está en las favoritas del usuario"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido o caducado"),
            @ApiResponse(responseCode = "404", description = "La película no existe"),
            @ApiResponse(responseCode = "409", description = "Dos marcas simultáneas de la misma película")
    })
    @PutMapping("/api/peliculas/{movieId}/favorita")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markFavorite(@PathVariable("movieId") Long movieId);

    @Operation(summary = "Quita una película de las favoritas; si no lo era, no hace nada")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "La película no está en las favoritas del usuario"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido o caducado")
    })
    @DeleteMapping("/api/peliculas/{movieId}/favorita")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unmarkFavorite(@PathVariable("movieId") Long movieId);

    @Operation(summary = "Marca una película como vista; si ya lo estaba, no hace nada")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "La película está en las vistas del usuario"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido o caducado"),
            @ApiResponse(responseCode = "404", description = "La película no existe"),
            @ApiResponse(responseCode = "409", description = "Dos marcas simultáneas de la misma película")
    })
    @PutMapping("/api/peliculas/{movieId}/vista")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markWatched(@PathVariable("movieId") Long movieId);

    @Operation(summary = "Quita una película de las vistas; si no lo estaba, no hace nada")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "La película no está en las vistas del usuario"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido o caducado")
    })
    @DeleteMapping("/api/peliculas/{movieId}/vista")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unmarkWatched(@PathVariable("movieId") Long movieId);

    @Operation(summary = "Películas favoritas del usuario autenticado, ordenadas por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Las favoritas que siguen existiendo en el catálogo"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido o caducado")
    })
    @GetMapping("/api/usuarios/me/favoritas")
    @ResponseStatus(HttpStatus.OK)
    List<MarkedMovieResponse> listFavorites();

    @Operation(summary = "Películas vistas del usuario autenticado, ordenadas por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Las vistas que siguen existiendo en el catálogo"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido o caducado")
    })
    @GetMapping("/api/usuarios/me/vistas")
    @ResponseStatus(HttpStatus.OK)
    List<MarkedMovieResponse> listWatched();
}

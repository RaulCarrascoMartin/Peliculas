package shop.catalog.api.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import shop.catalog.api.http.dto.MoviePageResponse;
import shop.catalog.api.http.dto.MovieRequest;
import shop.catalog.api.http.dto.MovieResponse;

import java.util.List;

/**
 * HTTP contract of the movie catalog, written by hand (this module has no yml): the single source of truth of its
 * routes, parameters, format validation and status codes. Errors are {@code application/problem+json}.
 */
@Tag(name = "Movie", description = "Catálogo de películas")
public interface MovieHttpApi {

    int MAX_PAGE_SIZE = 2000;

    @Operation(summary = "Busca películas, paginadas",
            description = "Público. Los filtros vacíos se ignoran y los indicados se combinan con «y».")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de películas (vacía si no hay coincidencias)"),
            @ApiResponse(responseCode = "400",
                    description = "Paginación, año o propiedad de ordenación no válidos")
    })
    @GetMapping("/api/peliculas")
    MoviePageResponse searchMovies(
            @Parameter(description = "Fragmento del título, sin distinguir mayúsculas")
            @RequestParam(name = "titulo", required = false) String title,
            @RequestParam(name = "genero", required = false) String genre,
            @RequestParam(name = "anio", required = false) Integer year,
            @RequestParam(name = "director", required = false) String director,
            @Parameter(description = "Número de página, desde 0")
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @Parameter(description = "Repetible. «propiedad» ordena ascendente; con varios «sort» cada uno admite "
                    + "«propiedad,asc|desc». Propiedades: id, title, year, genre, director, averageRating, price, "
                    + "stock")
            @RequestParam(name = "sort", defaultValue = "id") List<String> sort);

    @Operation(summary = "Detalle de una película", description = "Público.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La película"),
            @ApiResponse(responseCode = "400", description = "Identificador con formato no válido"),
            @ApiResponse(responseCode = "404", description = "La película no existe")
    })
    @GetMapping("/api/peliculas/{id}")
    MovieResponse getMovie(@PathVariable("id") Long id);

    @Operation(summary = "Crea una película", description = "Solo administradores.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Película creada"),
            @ApiResponse(responseCode = "400", description = "Datos no válidos"),
            @ApiResponse(responseCode = "401", description = "Sin identidad"),
            @ApiResponse(responseCode = "403", description = "No es administrador")
    })
    @PostMapping("/api/peliculas")
    ResponseEntity<MovieResponse> createMovie(@Valid @RequestBody MovieRequest request);

    @Operation(summary = "Modifica una película",
            description = "Solo administradores. Sin «stock» se conserva el que tenía; la valoración media no se edita.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Película modificada"),
            @ApiResponse(responseCode = "400", description = "Datos no válidos"),
            @ApiResponse(responseCode = "401", description = "Sin identidad"),
            @ApiResponse(responseCode = "403", description = "No es administrador"),
            @ApiResponse(responseCode = "404", description = "La película no existe")
    })
    @PutMapping("/api/peliculas/{id}")
    MovieResponse updateMovie(@PathVariable("id") Long id, @Valid @RequestBody MovieRequest request);

    @Operation(summary = "Elimina una película", description = "Solo administradores.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Película eliminada"),
            @ApiResponse(responseCode = "400", description = "Identificador con formato no válido"),
            @ApiResponse(responseCode = "401", description = "Sin identidad"),
            @ApiResponse(responseCode = "403", description = "No es administrador"),
            @ApiResponse(responseCode = "404", description = "La película no existe")
    })
    @DeleteMapping("/api/peliculas/{id}")
    ResponseEntity<Void> deleteMovie(@PathVariable("id") Long id);
}

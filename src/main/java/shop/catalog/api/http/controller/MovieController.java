package shop.catalog.api.http.controller;

import shop.catalog.api.http.dto.MovieRequest;
import shop.catalog.api.http.dto.MovieResponse;

import shop.catalog.api.http.dto.MoviePage;
import shop.shared.pagination.PageQuery;
import shop.shared.error.BusinessValidationException;
import shop.shared.pagination.PageResult;
import jakarta.validation.Valid;
import java.util.List;
import shop.catalog.internal.application.usecase.SearchMovies;
import shop.catalog.internal.application.usecase.CreateMovie;
import shop.catalog.internal.application.usecase.UpdateMovie;
import shop.catalog.internal.application.usecase.DeleteMovie;
import shop.catalog.internal.application.usecase.GetMovie;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/peliculas")
public class MovieController {

    private static final String ADMIN_ONLY = "hasRole('ADMIN')";
    private static final int MAX_PAGE_SIZE = 2000;

    private final SearchMovies searchMovies;
    private final GetMovie getMovie;
    private final CreateMovie createMovie;
    private final UpdateMovie updateMovie;
    private final DeleteMovie deleteMovie;

    public MovieController(SearchMovies searchMovies, GetMovie getMovie, CreateMovie createMovie,
                           UpdateMovie updateMovie, DeleteMovie deleteMovie) {
        this.searchMovies = searchMovies;
        this.getMovie = getMovie;
        this.createMovie = createMovie;
        this.updateMovie = updateMovie;
        this.deleteMovie = deleteMovie;
    }

    @GetMapping
    public MoviePage searchMovies(@RequestParam(name = "titulo", required = false) String title,
                                  @RequestParam(name = "genero", required = false) String genre,
                                  @RequestParam(name = "anio", required = false) Integer year,
                                  @RequestParam(required = false) String director,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(name = "sort", defaultValue = "id") List<String> sort) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessValidationException("Paginación no válida");
        }
        PageQuery query = new PageQuery(page, size, sort.stream().map(MovieController::toSort).toList());
        PageResult<MovieResponse> result = searchMovies.execute(title, genre, year, director, query)
                .map(MovieResponse::from);
        return MoviePage.from(result);
    }

    /** {@code property} or {@code property,asc|desc}; ascending by default. */
    private static PageQuery.Sort toSort(String param) {
        String[] parts = param.split(",");
        boolean descending = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc");
        return new PageQuery.Sort(parts[0].trim(), !descending);
    }

    @GetMapping("/{id}")
    public MovieResponse getMovie(@PathVariable Long id) {
        return MovieResponse.from(getMovie.execute(id));
    }

    @PostMapping
    @PreAuthorize(ADMIN_ONLY)
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse createMovie(@Valid @RequestBody MovieRequest request) {
        return MovieResponse.from(createMovie.execute(request.toMovieData()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ADMIN_ONLY)
    public MovieResponse updateMovie(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
        return MovieResponse.from(updateMovie.execute(id, request.toMovieData()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ADMIN_ONLY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMovie(@PathVariable Long id) {
        deleteMovie.execute(id);
    }
}

package shop.catalog.api.http.controller;

import shop.catalog.api.http.dto.MovieRequest;
import shop.catalog.api.http.dto.MovieResponse;

import org.springframework.data.domain.PageImpl;
import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;
import jakarta.validation.Valid;
import shop.catalog.internal.application.usecase.SearchMovies;
import shop.catalog.internal.application.usecase.CreateMovie;
import shop.catalog.internal.application.usecase.UpdateMovie;
import shop.catalog.internal.application.usecase.DeleteMovie;
import shop.catalog.internal.application.usecase.GetMovie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
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
    public PagedModel<MovieResponse> searchMovies(@RequestParam(name = "titulo", required = false) String title,
                                               @RequestParam(name = "genero", required = false) String genre,
                                               @RequestParam(name = "anio", required = false) Integer year,
                                               @RequestParam(required = false) String director,
                                               @PageableDefault(sort = "id") Pageable pageable) {
        PageQuery query = new PageQuery(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().stream().map(o -> new PageQuery.Sort(o.getProperty(), o.isAscending())).toList());
        PageResult<MovieResponse> result = searchMovies.execute(title, genre, year, director, query)
                .map(MovieResponse::from);
        return new PagedModel<>(new PageImpl<>(result.content(), pageable, result.total()));
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

package shop.catalog.api.http.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import shop.catalog.api.http.MovieHttpApi;
import shop.catalog.api.http.dto.MoviePageResponse;
import shop.catalog.api.http.dto.MovieRequest;
import shop.catalog.api.http.dto.MovieResponse;
import shop.catalog.api.http.mapper.MovieWebMapper;
import shop.catalog.internal.domain.service.MovieService;

import java.util.List;

/** HTTP entry of the catalog: implements the contract {@link MovieHttpApi} and delegates. */
@RestController
public class MovieController implements MovieHttpApi {

    private static final String ADMIN_ONLY = "hasRole('ADMIN')";

    private final MovieService movieService;
    private final MovieWebMapper mapper;

    public MovieController(MovieService movieService, MovieWebMapper mapper) {
        this.movieService = movieService;
        this.mapper = mapper;
    }

    @Override
    public MoviePageResponse searchMovies(String title, String genre, Integer year, String director,
                                          int page, int size, List<String> sort) {
        return mapper.toPageResponse(
                movieService.search(title, genre, year, director, mapper.toPageQuery(page, size, sort)));
    }

    @Override
    public MovieResponse getMovie(Long id) {
        return mapper.toResponse(movieService.find(id));
    }

    @Override
    @PreAuthorize(ADMIN_ONLY)
    public ResponseEntity<MovieResponse> createMovie(MovieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(movieService.create(mapper.toDto(request))));
    }

    @Override
    @PreAuthorize(ADMIN_ONLY)
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        return mapper.toResponse(movieService.update(id, mapper.toDto(request)));
    }

    @Override
    @PreAuthorize(ADMIN_ONLY)
    public ResponseEntity<Void> deleteMovie(Long id) {
        movieService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

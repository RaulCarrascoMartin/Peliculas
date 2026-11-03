package shop.rating.internal.web;

import shop.rating.internal.usecase.UnmarkMovie;
import shop.rating.internal.usecase.ListMarkedMovies;
import shop.rating.internal.usecase.MarkMovie;
import shop.rating.internal.domain.MarkType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MovieMarkController {

    private final MarkMovie markMovie;
    private final UnmarkMovie unmarkMovie;
    private final ListMarkedMovies listMarkedMovies;

    public MovieMarkController(MarkMovie markMovie, UnmarkMovie unmarkMovie,
                               ListMarkedMovies listMarkedMovies) {
        this.markMovie = markMovie;
        this.unmarkMovie = unmarkMovie;
        this.listMarkedMovies = listMarkedMovies;
    }

    @PutMapping("/api/peliculas/{movieId}/favorita")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markFavorite(@PathVariable Long movieId) {
        markMovie.execute(movieId, MarkType.FAVORITA);
    }

    @DeleteMapping("/api/peliculas/{movieId}/favorita")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unmarkFavorite(@PathVariable Long movieId) {
        unmarkMovie.execute(movieId, MarkType.FAVORITA);
    }

    @PutMapping("/api/peliculas/{movieId}/vista")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markWatched(@PathVariable Long movieId) {
        markMovie.execute(movieId, MarkType.VISTA);
    }

    @DeleteMapping("/api/peliculas/{movieId}/vista")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unmarkWatched(@PathVariable Long movieId) {
        unmarkMovie.execute(movieId, MarkType.VISTA);
    }

    @GetMapping("/api/usuarios/me/favoritas")
    public List<MarkedMovieResponse> listFavorites() {
        return listMarked(MarkType.FAVORITA);
    }

    @GetMapping("/api/usuarios/me/vistas")
    public List<MarkedMovieResponse> listWatched() {
        return listMarked(MarkType.VISTA);
    }

    private List<MarkedMovieResponse> listMarked(MarkType type) {
        return listMarkedMovies.execute(type).stream().map(MarkedMovieResponse::from).toList();
    }
}

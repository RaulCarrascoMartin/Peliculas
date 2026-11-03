package shop.rating.internal.usecase;

import shop.shared.error.ResourceNotFoundException;
import shop.shared.security.CurrentUser;
import shop.catalog.api.CatalogApi;
import shop.rating.internal.domain.MovieMark;
import shop.rating.internal.domain.MovieMarkRepository;
import shop.rating.internal.domain.MarkType;
import org.springframework.stereotype.Service;

@Service
public class MarkMovie {

    private final MovieMarkRepository marks;
    private final CatalogApi movies;
    private final CurrentUser currentUser;

    public MarkMovie(MovieMarkRepository marks, CatalogApi movies, CurrentUser currentUser) {
        this.marks = marks;
        this.movies = movies;
        this.currentUser = currentUser;
    }

    /** Adds the movie to the list; if it was already there, does nothing. */
    public void execute(Long movieId, MarkType type) {
        if (!movies.exists(movieId)) {
            throw new ResourceNotFoundException("Película " + movieId + " no encontrada");
        }
        Long userId = currentUser.id();
        if (marks.findByUserIdAndMovieIdAndType(userId, movieId, type).isEmpty()) {
            MovieMark mark = new MovieMark();
            mark.setUserId(userId);
            mark.setMovieId(movieId);
            mark.setType(type);
            marks.save(mark);
        }
    }
}

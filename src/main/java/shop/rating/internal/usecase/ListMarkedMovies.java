package shop.rating.internal.usecase;

import shop.shared.security.CurrentUser;
import shop.catalog.api.CatalogApi;
import shop.catalog.api.MovieSummary;
import shop.rating.internal.domain.MovieMark;
import shop.rating.internal.domain.MovieMarkRepository;
import shop.rating.internal.domain.MarkType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListMarkedMovies {

    private final MovieMarkRepository marks;
    private final CatalogApi movies;
    private final CurrentUser currentUser;

    public ListMarkedMovies(MovieMarkRepository marks, CatalogApi movies, CurrentUser currentUser) {
        this.marks = marks;
        this.movies = movies;
        this.currentUser = currentUser;
    }

    /** Movies in the current user's list, sorted by id. */
    public List<MovieSummary> execute(MarkType type) {
        List<Long> ids = marks.findByUserIdAndType(currentUser.id(), type).stream()
                .map(MovieMark::getMovieId)
                .toList();
        return movies.findSummaries(ids);
    }
}

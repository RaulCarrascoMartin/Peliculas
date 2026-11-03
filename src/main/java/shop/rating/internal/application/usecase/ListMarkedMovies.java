package shop.rating.internal.application.usecase;

import shop.rating.internal.application.dto.MarkedMovieView;

import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.service.MovieMarkService;

import java.util.List;

public class ListMarkedMovies {

    private final MovieMarkService marks;

    public ListMarkedMovies(MovieMarkService marks) {
        this.marks = marks;
    }

    /** Movies in the user's list, sorted by id. */
    public List<MarkedMovieView> execute(Long userId, MarkType type) {
        return marks.listMarked(userId, type).stream()
                .map(m -> new MarkedMovieView(m.id(), m.title(), m.year(), m.genre(), m.director(), m.averageRating()))
                .toList();
    }
}

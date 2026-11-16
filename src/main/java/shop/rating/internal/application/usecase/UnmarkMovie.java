package shop.rating.internal.application.usecase;

import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.service.MovieMarkService;

public class UnmarkMovie {

    private final MovieMarkService marks;

    public UnmarkMovie(MovieMarkService marks) {
        this.marks = marks;
    }

    public void execute(Long userId, Long movieId, MarkType type) {
        marks.unmark(userId, movieId, type);
    }
}

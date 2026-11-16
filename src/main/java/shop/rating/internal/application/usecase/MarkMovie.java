package shop.rating.internal.application.usecase;

import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.service.MovieMarkService;

public class MarkMovie {

    private final MovieMarkService marks;

    public MarkMovie(MovieMarkService marks) {
        this.marks = marks;
    }

    public void execute(Long userId, Long movieId, MarkType type) {
        marks.mark(userId, movieId, type);
    }
}

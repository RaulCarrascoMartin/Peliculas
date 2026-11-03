package shop.rating.internal.usecase;

import shop.shared.security.CurrentUser;
import shop.rating.internal.domain.MovieMarkRepository;
import shop.rating.internal.domain.MarkType;
import org.springframework.stereotype.Service;

@Service
public class UnmarkMovie {

    private final MovieMarkRepository marks;
    private final CurrentUser currentUser;

    public UnmarkMovie(MovieMarkRepository marks, CurrentUser currentUser) {
        this.marks = marks;
        this.currentUser = currentUser;
    }

    /** Removes the movie from the list; if it was not there, does nothing. */
    public void execute(Long movieId, MarkType type) {
        marks.findByUserIdAndMovieIdAndType(currentUser.id(), movieId, type)
                .ifPresent(marks::delete);
    }
}

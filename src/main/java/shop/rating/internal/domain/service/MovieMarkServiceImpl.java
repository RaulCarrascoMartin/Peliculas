package shop.rating.internal.domain.service;

import shop.rating.internal.domain.contract.CatalogMovie;
import shop.rating.internal.domain.contract.MovieCatalog;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.model.MovieMark;
import shop.rating.internal.domain.repository.MovieMarkRepository;

import java.util.List;

public class MovieMarkServiceImpl implements MovieMarkService {

    private final MovieMarkRepository marks;
    private final MovieCatalog movies;

    public MovieMarkServiceImpl(MovieMarkRepository marks, MovieCatalog movies) {
        this.marks = marks;
        this.movies = movies;
    }

    @Override
    public void mark(Long userId, Long movieId, MarkType type) {
        if (!movies.exists(movieId)) {
            throw new MovieNotFoundException(movieId);
        }
        if (marks.findByUserIdAndMovieIdAndType(userId, movieId, type).isEmpty()) {
            MovieMark mark = new MovieMark();
            mark.setUserId(userId);
            mark.setMovieId(movieId);
            mark.setType(type);
            marks.save(mark);
        }
    }

    @Override
    public void unmark(Long userId, Long movieId, MarkType type) {
        marks.findByUserIdAndMovieIdAndType(userId, movieId, type).ifPresent(marks::delete);
    }

    @Override
    public List<CatalogMovie> listMarked(Long userId, MarkType type) {
        List<Long> ids = marks.findByUserIdAndType(userId, type).stream().map(MovieMark::getMovieId).toList();
        return movies.findByIds(ids);
    }
}

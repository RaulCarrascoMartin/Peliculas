package shop.rating.internal.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.rating.internal.domain.contract.MovieCatalog;
import shop.rating.internal.domain.dto.CatalogMovieDto;
import shop.rating.internal.domain.dto.MovieMarkDto;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.mapper.MovieMarkMapper;
import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.model.MovieMark;
import shop.rating.internal.domain.repository.MovieMarkRepository;

import java.util.List;

/** The uniqueness of a mark is also guaranteed by the table; the transaction covers the lookup plus the write. */
@Service
public class MovieMarkServiceImpl implements MovieMarkService {

    private final MovieMarkRepository marks;
    private final MovieCatalog movies;
    private final MovieMarkMapper mapper;

    public MovieMarkServiceImpl(MovieMarkRepository marks, MovieCatalog movies, MovieMarkMapper mapper) {
        this.marks = marks;
        this.movies = movies;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void mark(MovieMarkDto request) {
        if (!movies.exists(request.movieId())) {
            throw new MovieNotFoundException(request.movieId());
        }
        if (marks.findByUserIdAndMovieIdAndType(request.userId(), request.movieId(), request.type()).isEmpty()) {
            MovieMark mark = new MovieMark();
            mark.setUserId(request.userId());
            mark.setMovieId(request.movieId());
            mark.setType(request.type());
            marks.save(mapper.toDto(mark));
        }
    }

    @Override
    @Transactional
    public void unmark(MovieMarkDto request) {
        marks.findByUserIdAndMovieIdAndType(request.userId(), request.movieId(), request.type())
                .map(mapper::toModel)
                .ifPresent(mark -> marks.delete(mark.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogMovieDto> listMarked(Long userId, MarkType type) {
        List<Long> ids = marks.findByUserIdAndType(userId, type).stream().map(MovieMarkDto::movieId).toList();
        return movies.findByIds(ids);
    }
}

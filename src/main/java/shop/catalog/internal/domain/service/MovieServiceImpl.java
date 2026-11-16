package shop.catalog.internal.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.catalog.internal.domain.dto.MovieDetailsDto;
import shop.catalog.internal.domain.dto.MovieDto;
import shop.catalog.internal.domain.exception.InvalidSortPropertyException;
import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.mapper.MovieMapper;
import shop.catalog.internal.domain.model.Movie;
import shop.catalog.internal.domain.repository.MovieRepository;
import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class MovieServiceImpl implements MovieService {

    /** Properties of a movie the search can be sorted by; the sort comes from the client, so it is a closed list. */
    private static final Set<String> SORTABLE = Set.of(
            "id", "title", "year", "genre", "director", "averageRating", "price", "stock");

    private final MovieRepository movies;
    private final MovieMapper mapper;

    public MovieServiceImpl(MovieRepository movies, MovieMapper mapper) {
        this.movies = movies;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public MovieDto create(MovieDetailsDto details) {
        Movie movie = new Movie();
        mapper.copyDetails(details, movie);
        movie.setStock(details.stock() == null ? 0 : details.stock());
        return movies.save(mapper.toDto(movie));
    }

    @Override
    @Transactional
    public MovieDto update(Long id, MovieDetailsDto details) {
        Movie movie = load(id);
        mapper.copyDetails(details, movie);
        if (details.stock() != null) {
            movie.setStock(details.stock());
        }
        return movies.save(mapper.toDto(movie));
    }

    @Override
    @Transactional(readOnly = true)
    public MovieDto find(Long id) {
        return mapper.toDto(load(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!movies.existsById(id)) {
            throw new MovieNotFoundException(id);
        }
        movies.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MovieDto> search(String title, String genre, Integer year, String director, PageQuery page) {
        for (PageQuery.Sort sort : page.sort()) {
            if (!SORTABLE.contains(sort.property())) {
                throw new InvalidSortPropertyException(sort.property());
            }
        }
        return movies.search(blankToNull(title), blankToNull(genre), year, blankToNull(director), page);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return movies.existsById(id);
    }

    @Override
    @Transactional
    public void updateAverageRating(Long id, Double average) {
        if (movies.updateAverageRating(id, average) == 0) {
            throw new MovieNotFoundException(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieDto> findAllById(Collection<Long> ids) {
        return movies.findAllById(ids).stream().sorted(Comparator.comparing(MovieDto::id)).toList();
    }

    private Movie load(Long id) {
        return movies.findById(id)
                .map(mapper::toModel)
                .orElseThrow(() -> new MovieNotFoundException(id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}

package shop.catalog.internal.domain.service;

import shop.catalog.internal.domain.dto.MovieDetailsDto;
import shop.catalog.internal.domain.dto.MovieDto;
import shop.catalog.internal.domain.exception.InvalidSortPropertyException;
import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;

import java.util.Collection;
import java.util.List;

/** Movie rules: CRUD, filtered search and the average rating computed by another module. */
public interface MovieService {

    /** A movie created without stock starts with 0 units. */
    MovieDto create(MovieDetailsDto details);

    /**
     * Replaces title, year, genre, director, price and (if given) stock; the average is not edited by hand.
     *
     * @throws MovieNotFoundException if it does not exist
     */
    MovieDto update(Long id, MovieDetailsDto details);

    /** @throws MovieNotFoundException if it does not exist */
    MovieDto find(Long id);

    /** @throws MovieNotFoundException if it does not exist */
    void delete(Long id);

    /**
     * Paged list; null or blank filters are ignored and the given ones are combined with "and". The page can be
     * sorted by {@code id, title, year, genre, director, averageRating, price, stock}.
     *
     * @throws InvalidSortPropertyException if the page asks to sort by any other property
     */
    PageResult<MovieDto> search(String title, String genre, Integer year, String director, PageQuery page);

    boolean exists(Long id);

    /** @throws MovieNotFoundException if it does not exist */
    void updateAverageRating(Long id, Double average);

    /** The movies that exist among the ids, sorted by id; unknown ids are ignored. */
    List<MovieDto> findAllById(Collection<Long> ids);
}

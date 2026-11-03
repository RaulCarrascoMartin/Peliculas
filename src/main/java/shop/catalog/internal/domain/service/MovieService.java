package shop.catalog.internal.domain.service;

import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.model.Movie;
import shop.catalog.internal.domain.model.MovieDetails;

import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;

import java.util.Collection;
import java.util.List;

/** Movie rules: CRUD, filtered search and the average rating computed by another module. */
public interface MovieService {

    Movie create(MovieDetails details);

    /** Replaces title, year, genre, director, price and (if given) stock; the average is not edited by hand. */
    Movie update(Long id, MovieDetails details);

    /** @throws MovieNotFoundException if it does not exist */
    Movie find(Long id);

    /** @throws MovieNotFoundException if it does not exist */
    void delete(Long id);

    /** Paged list; null or blank filters are ignored and the given ones are combined with "and". */
    PageResult<Movie> search(String title, String genre, Integer year, String director, PageQuery page);

    boolean exists(Long id);

    /** @throws MovieNotFoundException if it does not exist */
    void updateAverageRating(Long id, Double average);

    /** The movies that exist among the ids, sorted by id; unknown ids are ignored. */
    List<Movie> findAllById(Collection<Long> ids);
}

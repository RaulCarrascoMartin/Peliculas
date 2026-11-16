package shop.catalog.internal.domain.repository;

import shop.catalog.internal.domain.model.Movie;

import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Movie persistence as the catalog domain needs it; the implementation lives in {@code internal.persistence}. */
public interface MovieRepository {

    Movie save(Movie movie);

    Optional<Movie> findById(Long id);

    boolean existsById(Long id);

    void deleteById(Long id);

    /** Movies that exist among the given ids; unknown ids are ignored. */
    List<Movie> findAllById(Collection<Long> ids);

    /** {@code null} parameters do not filter; the title is matched by fragment, ignoring case. */
    PageResult<Movie> search(String title, String genre, Integer year, String director, PageQuery page);

    /** Subtracts units only if enough remain; returns the affected rows (0 if there was not enough stock). */
    int decrementStock(Long id, int quantity);

    /** Adds units to the stock; returns the affected rows (0 if the movie no longer exists). */
    int incrementStock(Long id, int quantity);
}

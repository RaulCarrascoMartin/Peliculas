package shop.catalog.internal.domain.repository;

import shop.catalog.internal.domain.dto.MovieDto;
import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Movie persistence as the catalog domain needs it; the implementation lives in {@code internal.persistence}. */
public interface MovieRepository {

    /** Inserts the movie if it has no id (and returns it with the assigned one) or updates its details and stock if it
     * has; the average rating is never written here, only by {@link #updateAverageRating}.
     *
     * @throws shop.catalog.internal.domain.exception.MovieNotFoundException if it has an id that does not exist
     */
    MovieDto save(MovieDto movie);

    Optional<MovieDto> findById(Long id);

    boolean existsById(Long id);

    void deleteById(Long id);

    /** Movies that exist among the given ids; unknown ids are ignored. */
    List<MovieDto> findAllById(Collection<Long> ids);

    /**
     * {@code null} parameters do not filter; the title is matched by fragment, ignoring case. The sort properties
     * of the page are the names of {@link MovieDto}'s components and must have been validated by the service.
     */
    PageResult<MovieDto> search(String title, String genre, Integer year, String director, PageQuery page);

    /** Sets the average rating without touching the other columns; returns the affected rows (0 if missing). */
    int updateAverageRating(Long id, Double average);

    /** Subtracts units only if enough remain; returns the affected rows (0 if there was not enough stock). */
    int decrementStock(Long id, int quantity);

    /** Adds units to the stock; returns the affected rows (0 if the movie no longer exists). */
    int incrementStock(Long id, int quantity);
}

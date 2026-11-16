package shop.rating.internal.domain.contract;

import shop.rating.internal.domain.dto.CatalogMovieDto;

import java.util.Collection;
import java.util.List;

/** What the rating module needs from the catalog; an adapter in {@code persistence} fulfils it. */
public interface MovieCatalog {

    boolean exists(Long movieId);

    /** Stores the average computed by this module in the catalog. */
    void updateAverageRating(Long movieId, double average);

    /** Data of the requested movies, sorted by id; unknown ids are ignored. */
    List<CatalogMovieDto> findByIds(Collection<Long> ids);
}

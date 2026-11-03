package shop.rating.internal.domain.contract;

import java.util.Collection;
import java.util.List;

/** What the rating module needs from the catalog; an adapter in {@code persistence} fulfils it. */
public interface MovieCatalog {

    boolean exists(Long movieId);

    /** Stores the average computed by this module in the catalog. */
    void updateAverageRating(Long movieId, double average);

    /** Data of the requested movies, sorted by id; unknown ids are ignored. */
    List<CatalogMovie> findByIds(Collection<Long> ids);
}

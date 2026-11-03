package shop.catalog.api;

import java.util.Collection;
import java.util.List;

/** Public API of the {@code catalog} module: the only thing other modules may use. */
public interface CatalogApi {

    boolean exists(Long id);

    /** Stores the average computed by another module; throws 404 if the movie does not exist. */
    void updateAverageRating(Long id, Double media);

    /** Data of the requested movies, sorted by id; unknown ids are ignored. */
    List<MovieSummary> findSummaries(Collection<Long> ids);

    /**
     * Reserves units of all the lines or of none: if one fails, the stock stays as it was.
     * Returns one line per requested line, with the title and price at that moment.
     *
     * @throws UnknownProductException        if any product does not exist
     * @throws ProductNotPurchasableException if any product has no price
     * @throws InsufficientStockException     if there are not enough units of any product
     */
    List<ReservedLine> reserve(List<StockLine> lines);

    /** Returns the given units to the stock; ignores products that no longer exist. */
    void release(List<StockLine> lines);
}

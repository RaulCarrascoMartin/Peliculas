package shop.cart.internal.usecase;

import shop.catalog.api.CatalogApi;
import shop.catalog.api.MovieSummary;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Test double of {@link CatalogApi}: remembers existing movies and prices, mutable for the live price. */
public class CatalogApiFake implements CatalogApi {

    private final Map<Long, BigDecimal> prices = new HashMap<>();

    public void createMovie(Long id, BigDecimal price) {
        prices.put(id, price);
    }

    /** Changes the price of an existing movie, like the {@code catalog} admin. */
    public void changePrice(Long id, BigDecimal price) {
        prices.put(id, price);
    }

    @Override
    public boolean exists(Long id) {
        return prices.containsKey(id);
    }

    @Override
    public void updateAverageRating(Long id, Double media) {
        // the cart never updates averages; this module does not consume it
    }

    /** Data of the requested movies, sorted by id; unknown ids are ignored. */
    @Override
    public List<MovieSummary> findSummaries(Collection<Long> ids) {
        return ids.stream().filter(this::exists).sorted()
                .map(id -> new MovieSummary(id, "Película " + id, 2000, "Drama", "Director",
                        prices.get(id), null, 0))
                .toList();
    }

    @Override
    public java.util.List<shop.catalog.api.ReservedLine> reserve(java.util.List<shop.catalog.api.StockLine> lines) {
        throw new UnsupportedOperationException("This module does not use it");
    }

    @Override
    public void release(java.util.List<shop.catalog.api.StockLine> lines) {
        throw new UnsupportedOperationException("This module does not use it");
    }
}

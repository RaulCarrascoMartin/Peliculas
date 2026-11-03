package shop.purchase.internal.usecase;

import shop.catalog.api.CatalogApi;
import shop.catalog.api.MovieSummary;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Test double of {@link CatalogApi}: mutable titles and prices, as the admin edits them. */
public class CatalogApiFake implements CatalogApi {

    private final Map<Long, String> titles = new HashMap<>();
    private final Map<Long, BigDecimal> prices = new HashMap<>();

    public void createMovie(Long id, String title, BigDecimal price) {
        titles.put(id, title);
        prices.put(id, price);
    }

    public void changePrice(Long id, BigDecimal price) {
        prices.put(id, price);
    }

    public void changeTitle(Long id, String title) {
        titles.put(id, title);
    }

    public void delete(Long id) {
        titles.remove(id);
        prices.remove(id);
    }

    @Override
    public boolean exists(Long id) {
        return titles.containsKey(id);
    }

    @Override
    public void updateAverageRating(Long id, Double media) {
        // the purchase never updates averages; this module does not consume it
    }

    @Override
    public List<MovieSummary> findSummaries(Collection<Long> ids) {
        return ids.stream().filter(this::exists).sorted()
                .map(id -> new MovieSummary(id, titles.get(id), 2000, "Drama", "Director",
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

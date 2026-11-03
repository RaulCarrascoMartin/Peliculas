package shop.rating.internal.usecase;

import shop.shared.error.ResourceNotFoundException;
import shop.catalog.api.CatalogApi;
import shop.catalog.api.MovieSummary;

import java.util.HashMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Test double of {@link CatalogApi}: remembers the "existing" movies and the saved averages. */
public class CatalogApiFake implements CatalogApi {

    private final Set<Long> existing = new HashSet<>();
    private final Map<Long, Double> averages = new HashMap<>();

    public void addMovie(Long id) {
        existing.add(id);
    }

    public Double averageOf(Long id) {
        return averages.get(id);
    }

    @Override
    public boolean exists(Long id) {
        return existing.contains(id);
    }

    @Override
    public void updateAverageRating(Long id, Double media) {
        if (!exists(id)) {
            throw new ResourceNotFoundException("Película " + id + " no encontrada");
        }
        averages.put(id, media);
    }

    @Override
    public List<MovieSummary> findSummaries(Collection<Long> ids) {
        return ids.stream().filter(this::exists).sorted()
                .map(id -> new MovieSummary(id, "Película " + id, 2000, "Drama", "Director",
                        new java.math.BigDecimal("12.99"), averages.get(id), 0))
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

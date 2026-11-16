package shop.rating.internal.domain.fake;

import shop.rating.internal.domain.contract.CatalogMovie;
import shop.rating.internal.domain.contract.MovieCatalog;
import shop.shared.error.ResourceNotFoundException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Test double of {@link MovieCatalog}: remembers the "existing" movies and the saved averages. */
public class MovieCatalogFake implements MovieCatalog {

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
    public void updateAverageRating(Long id, double average) {
        if (!exists(id)) {
            throw new ResourceNotFoundException("Película " + id + " no encontrada");
        }
        averages.put(id, average);
    }

    @Override
    public List<CatalogMovie> findByIds(Collection<Long> ids) {
        return ids.stream().filter(this::exists).sorted()
                .map(id -> new CatalogMovie(id, "Película " + id, 2000, "Drama", "Director", averages.get(id)))
                .toList();
    }
}

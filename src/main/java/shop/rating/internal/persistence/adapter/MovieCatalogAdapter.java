package shop.rating.internal.persistence.adapter;

import shop.catalog.api.facade.CatalogFacade;
import shop.rating.internal.domain.contract.CatalogMovie;
import shop.rating.internal.domain.contract.MovieCatalog;

import java.util.Collection;
import java.util.List;

/** Translates the rating's {@link MovieCatalog} contract to the public facade of {@code catalog}. */
public class MovieCatalogAdapter implements MovieCatalog {

    private final CatalogFacade catalog;

    public MovieCatalogAdapter(CatalogFacade catalog) {
        this.catalog = catalog;
    }

    @Override
    public boolean exists(Long movieId) {
        return catalog.exists(movieId);
    }

    @Override
    public void updateAverageRating(Long movieId, double average) {
        catalog.updateAverageRating(movieId, average);
    }

    @Override
    public List<CatalogMovie> findByIds(Collection<Long> ids) {
        return catalog.findSummaries(ids).stream()
                .map(m -> new CatalogMovie(m.id(), m.title(), m.year(), m.genre(), m.director(), m.averageRating()))
                .toList();
    }
}

package shop.rating.internal.persistence.adapter;

import jakarta.inject.Named;
import shop.catalog.api.facade.CatalogFacade;
import shop.rating.internal.domain.contract.MovieCatalog;
import shop.rating.internal.domain.dto.CatalogMovieDto;

import java.util.Collection;
import java.util.List;

/**
 * Translates the rating's {@link MovieCatalog} contract to the public facade of {@code catalog}. The bean is named
 * explicitly because cart and purchase have an adapter with the same simple name.
 */
@Named("ratingMovieCatalogAdapter")
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
    public List<CatalogMovieDto> findByIds(Collection<Long> ids) {
        return catalog.findSummaries(ids).stream()
                .map(m -> new CatalogMovieDto(m.id(), m.title(), m.year(), m.genre(), m.director(), m.averageRating()))
                .toList();
    }
}

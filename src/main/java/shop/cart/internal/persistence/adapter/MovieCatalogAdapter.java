package shop.cart.internal.persistence.adapter;

import jakarta.inject.Named;
import shop.cart.internal.domain.contract.MovieCatalog;
import shop.cart.internal.domain.dto.CatalogMovieDto;
import shop.catalog.api.facade.CatalogFacade;

import java.util.Collection;
import java.util.List;

/**
 * Translates the cart's {@link MovieCatalog} contract to the public facade of {@code catalog}. The bean is named
 * explicitly because purchase and rating have an adapter with the same simple name.
 */
@Named("cartMovieCatalogAdapter")
public class MovieCatalogAdapter implements MovieCatalog {

    private final CatalogFacade catalog;

    public MovieCatalogAdapter(CatalogFacade catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<CatalogMovieDto> findByIds(Collection<Long> ids) {
        return catalog.findSummaries(ids).stream()
                .map(m -> new CatalogMovieDto(m.id(), m.title(), m.price()))
                .toList();
    }
}

package shop.cart.internal.persistence.adapter;

import shop.cart.internal.domain.contract.CatalogMovie;
import shop.cart.internal.domain.contract.MovieCatalog;
import shop.catalog.api.facade.CatalogFacade;

import java.util.Collection;
import java.util.List;

/** Translates the cart's {@link MovieCatalog} contract to the public facade of {@code catalog}. */
public class MovieCatalogAdapter implements MovieCatalog {

    private final CatalogFacade catalog;

    public MovieCatalogAdapter(CatalogFacade catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<CatalogMovie> findByIds(Collection<Long> ids) {
        return catalog.findSummaries(ids).stream()
                .map(m -> new CatalogMovie(m.id(), m.title(), m.price()))
                .toList();
    }
}

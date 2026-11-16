package shop.cart.internal.domain.contract;

import shop.cart.internal.domain.dto.CatalogMovieDto;

import java.util.Collection;
import java.util.List;

/** What the cart needs to know about the catalog; the cart defines it, an adapter in {@code persistence} fulfils it. */
public interface MovieCatalog {

    /** Data of the requested movies, sorted by id; unknown ids are ignored. */
    List<CatalogMovieDto> findByIds(Collection<Long> ids);
}

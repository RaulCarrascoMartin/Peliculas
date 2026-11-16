package shop.purchase.internal.domain.contract;

import shop.purchase.internal.domain.dto.CatalogMovieDto;

import java.util.Collection;
import java.util.List;

/** What the purchase needs to know about the catalog; an adapter in {@code persistence} fulfils it. */
public interface MovieCatalog {

    /** Data of the requested movies; unknown ids are ignored. */
    List<CatalogMovieDto> findByIds(Collection<Long> ids);
}

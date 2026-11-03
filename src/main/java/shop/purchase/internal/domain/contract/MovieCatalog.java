package shop.purchase.internal.domain.contract;

import java.util.Collection;
import java.util.List;

/** What the purchase needs to know about the catalog; an adapter in {@code persistence} fulfils it. */
public interface MovieCatalog {

    /** Data of the requested movies; unknown ids are ignored. */
    List<CatalogMovie> findByIds(Collection<Long> ids);
}

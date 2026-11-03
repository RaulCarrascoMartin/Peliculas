package shop.purchase.internal.domain.fake;

import shop.purchase.internal.domain.contract.CatalogMovie;
import shop.purchase.internal.domain.contract.MovieCatalog;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Test double of {@link MovieCatalog}: mutable titles and prices, as the admin edits them. */
public class MovieCatalogFake implements MovieCatalog {

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
    public List<CatalogMovie> findByIds(Collection<Long> ids) {
        return ids.stream().filter(titles::containsKey).sorted()
                .map(id -> new CatalogMovie(id, titles.get(id), prices.get(id)))
                .toList();
    }
}

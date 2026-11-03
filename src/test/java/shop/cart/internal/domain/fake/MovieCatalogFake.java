package shop.cart.internal.domain.fake;

import shop.cart.internal.domain.contract.CatalogMovie;
import shop.cart.internal.domain.contract.MovieCatalog;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Test double of {@link MovieCatalog}: remembers existing movies and prices, mutable for the live price. */
public class MovieCatalogFake implements MovieCatalog {

    private final Map<Long, BigDecimal> prices = new HashMap<>();

    public void createMovie(Long id, BigDecimal price) {
        prices.put(id, price);
    }

    /** Changes the price of an existing movie, like the {@code catalog} admin. */
    public void changePrice(Long id, BigDecimal price) {
        prices.put(id, price);
    }

    @Override
    public List<CatalogMovie> findByIds(Collection<Long> ids) {
        return ids.stream().filter(prices::containsKey).sorted()
                .map(id -> new CatalogMovie(id, "Película " + id, prices.get(id)))
                .toList();
    }
}

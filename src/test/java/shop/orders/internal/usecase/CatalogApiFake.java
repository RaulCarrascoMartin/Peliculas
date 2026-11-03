package shop.orders.internal.usecase;

import shop.catalog.api.CatalogApi;
import shop.catalog.api.InsufficientStockException;
import shop.catalog.api.MovieSummary;
import shop.catalog.api.ReservedLine;
import shop.catalog.api.StockLine;
import shop.catalog.api.UnknownProductException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Double of {@link CatalogApi} to test {@code orders} in isolation: products with title, price and stock. */
public class CatalogApiFake implements CatalogApi {

    private final Map<Long, String> titles = new HashMap<>();
    private final Map<Long, BigDecimal> prices = new HashMap<>();
    private final Map<Long, Integer> stock = new HashMap<>();

    public void createProduct(Long id, String title, String price, int units) {
        titles.put(id, title);
        prices.put(id, new BigDecimal(price));
        stock.put(id, units);
    }

    public void changePrice(Long id, String price) {
        prices.put(id, new BigDecimal(price));
    }

    public int stockOf(Long id) {
        return stock.get(id);
    }

    @Override
    public List<ReservedLine> reserve(List<StockLine> lines) {
        // All or nothing: everything is validated before deducting anything.
        for (StockLine l : lines) {
            if (!stock.containsKey(l.productId())) {
                throw new UnknownProductException(l.productId());
            }
        }
        Map<Long, Integer> quantities = new HashMap<>();
        lines.forEach(l -> quantities.merge(l.productId(), l.quantity(), Integer::sum));
        quantities.forEach((id, quantity) -> {
            if (stock.get(id) < quantity) {
                throw new InsufficientStockException(id);
            }
        });
        quantities.forEach((id, quantity) -> stock.merge(id, -quantity, Integer::sum));
        return lines.stream()
                .map(l -> new ReservedLine(l.productId(), titles.get(l.productId()), prices.get(l.productId()),
                        l.quantity()))
                .toList();
    }

    @Override
    public void release(List<StockLine> lines) {
        lines.forEach(l -> stock.computeIfPresent(l.productId(), (id, actual) -> actual + l.quantity()));
    }

    @Override
    public boolean exists(Long id) {
        throw new UnsupportedOperationException("orders only uses reserve and release");
    }

    @Override
    public void updateAverageRating(Long id, Double media) {
        throw new UnsupportedOperationException("orders only uses reserve and release");
    }

    @Override
    public List<MovieSummary> findSummaries(Collection<Long> ids) {
        throw new UnsupportedOperationException("orders only uses reserve and release");
    }
}

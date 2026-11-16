package shop.orders.internal.domain.fake;

import shop.orders.internal.domain.contract.StockReservation;
import shop.shared.error.ConflictException;
import shop.shared.error.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Double of {@link StockReservation} to test {@code orders} in isolation: products with title, price and stock. */
public class StockReservationFake implements StockReservation {

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
    public List<Reserved> reserve(List<Item> items) {
        // All or nothing: everything is validated before deducting anything.
        for (Item i : items) {
            if (!stock.containsKey(i.productId())) {
                throw new ResourceNotFoundException("El producto " + i.productId() + " no existe");
            }
        }
        Map<Long, Integer> quantities = new HashMap<>();
        items.forEach(i -> quantities.merge(i.productId(), i.quantity(), Integer::sum));
        quantities.forEach((id, quantity) -> {
            if (stock.get(id) < quantity) {
                throw new ConflictException("No hay stock suficiente del producto " + id);
            }
        });
        quantities.forEach((id, quantity) -> stock.merge(id, -quantity, Integer::sum));
        return items.stream()
                .map(i -> new Reserved(i.productId(), titles.get(i.productId()), prices.get(i.productId()),
                        i.quantity()))
                .toList();
    }

    @Override
    public void release(List<Item> items) {
        items.forEach(i -> stock.computeIfPresent(i.productId(), (id, actual) -> actual + i.quantity()));
    }
}

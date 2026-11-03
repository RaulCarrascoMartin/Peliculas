package shop.orders.internal.usecase;

import shop.orders.internal.domain.Order;
import shop.orders.internal.domain.OrderRepository;
import shop.orders.internal.domain.OrderStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory double of {@link OrderRepository}. */
public class OrderRepositoryFake implements OrderRepository {

    private final Map<Long, Order> orders = new LinkedHashMap<>();
    private long nextId = 1;
    private boolean failOnSave;

    public void failOnSave() {
        this.failOnSave = true;
    }

    public int count() {
        return orders.size();
    }

    @Override
    public Order save(Order order) {
        if (failOnSave) {
            throw new IllegalStateException("simulated persistence failure");
        }
        order.setId(nextId++);
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public boolean cancelIfCreated(Long id) {
        Order order = orders.get(id);
        if (order == null || order.getStatus() != OrderStatus.CREATED) {
            return false;
        }
        order.setStatus(OrderStatus.CANCELLED);
        return true;
    }
}

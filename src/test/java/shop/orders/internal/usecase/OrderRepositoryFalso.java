package shop.orders.internal.usecase;

import shop.orders.internal.domain.Order;
import shop.orders.internal.domain.OrderRepository;
import shop.orders.internal.domain.OrderStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Doble en memoria de {@link OrderRepository}. */
public class OrderRepositoryFalso implements OrderRepository {

    private final Map<Long, Order> pedidos = new LinkedHashMap<>();
    private long siguienteId = 1;
    private boolean fallarAlGuardar;

    public void fallarAlGuardar() {
        this.fallarAlGuardar = true;
    }

    public int count() {
        return pedidos.size();
    }

    @Override
    public Order save(Order order) {
        if (fallarAlGuardar) {
            throw new IllegalStateException("fallo simulado de persistencia");
        }
        order.setId(siguienteId++);
        pedidos.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(pedidos.get(id));
    }

    @Override
    public boolean cancelIfCreated(Long id) {
        Order order = pedidos.get(id);
        if (order == null || order.getStatus() != OrderStatus.CREATED) {
            return false;
        }
        order.setStatus(OrderStatus.CANCELLED);
        return true;
    }
}

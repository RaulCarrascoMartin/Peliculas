package shop.orders.internal.usecase;

import org.springframework.stereotype.Service;
import shop.catalog.api.CatalogApi;
import shop.catalog.api.StockLine;
import shop.shared.error.ConflictException;
import shop.shared.error.ResourceNotFoundException;
import shop.orders.internal.domain.Order;
import shop.orders.internal.domain.OrderRepository;
import shop.orders.internal.domain.OrderStatus;

@Service
public class CancelOrder {

    private final CatalogApi catalog;
    private final OrderRepository orders;

    public CancelOrder(CatalogApi catalog, OrderRepository orders) {
        this.catalog = catalog;
        this.orders = orders;
    }

    /**
     * Only cancellable from CREATED. The status change is conditional: if two cancellations coincide,
     * only one succeeds and only that one returns the stock; the other gets a 409.
     */
    public OrderView execute(Long id) {
        Order order = orders.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El pedido " + id + " no existe"));
        if (order.getStatus() != OrderStatus.CREATED || !orders.cancelIfCreated(id)) {
            throw new ConflictException("El pedido " + id + " no está en estado CREATED");
        }
        catalog.release(order.getLines().stream()
                .map(l -> new StockLine(l.getProductId(), l.getQuantity()))
                .toList());
        order.setStatus(OrderStatus.CANCELLED);
        return OrderView.of(order);
    }
}

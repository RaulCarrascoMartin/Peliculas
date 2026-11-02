package shop.orders.internal.usecase;

import org.springframework.stereotype.Service;
import shop.catalog.api.CatalogApi;
import shop.catalog.api.StockLine;
import shop.compartido.error.ConflictoException;
import shop.compartido.error.RecursoNoEncontradoException;
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
     * Solo se cancela desde CREATED. El cambio de estado es condicional: si dos cancelaciones coinciden,
     * solo una lo consigue y solo esa devuelve el stock; la otra recibe un 409.
     */
    public OrderView execute(Long id) {
        Order order = orders.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El pedido " + id + " no existe"));
        if (order.getStatus() != OrderStatus.CREATED || !orders.cancelIfCreated(id)) {
            throw new ConflictoException("El pedido " + id + " no está en estado CREATED");
        }
        catalog.release(order.getLines().stream()
                .map(l -> new StockLine(l.getProductId(), l.getQuantity()))
                .toList());
        order.setStatus(OrderStatus.CANCELLED);
        return OrderView.of(order);
    }
}

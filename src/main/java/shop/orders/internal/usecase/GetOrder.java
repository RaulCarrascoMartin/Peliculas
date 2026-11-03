package shop.orders.internal.usecase;

import org.springframework.stereotype.Service;
import shop.shared.error.ResourceNotFoundException;
import shop.orders.internal.domain.OrderRepository;

@Service
public class GetOrder {

    private final OrderRepository orders;

    public GetOrder(OrderRepository orders) {
        this.orders = orders;
    }

    public OrderView execute(Long id) {
        return orders.findById(id)
                .map(OrderView::of)
                .orElseThrow(() -> new ResourceNotFoundException("El pedido " + id + " no existe"));
    }
}

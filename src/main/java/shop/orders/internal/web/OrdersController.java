package shop.orders.internal.web;

import org.springframework.web.bind.annotation.RestController;
import shop.orders.internal.usecase.CancelOrder;
import shop.orders.internal.usecase.CreateOrder;
import shop.orders.internal.usecase.GetOrder;
import shop.orders.internal.usecase.NewOrder;
import shop.orders.internal.usecase.NewOrderLine;
import shop.orders.internal.usecase.OrderLineView;
import shop.orders.internal.usecase.OrderView;
import shop.orders.internal.web.api.OrdersRestApi;
import shop.orders.internal.web.dto.Order;
import shop.orders.internal.web.dto.OrderLine;
import shop.orders.internal.web.dto.OrderRequest;

import java.time.ZoneOffset;
import java.util.List;

@RestController
public class OrdersController implements OrdersRestApi {

    private final CreateOrder createOrder;
    private final GetOrder getOrder;
    private final CancelOrder cancelOrder;

    public OrdersController(CreateOrder createOrder, GetOrder getOrder, CancelOrder cancelOrder) {
        this.createOrder = createOrder;
        this.getOrder = getOrder;
        this.cancelOrder = cancelOrder;
    }

    @Override
    public Order createOrder(OrderRequest orderRequest) {
        List<NewOrderLine> lines = orderRequest.lines().stream()
                .map(l -> new NewOrderLine(l.productId(), l.quantity()))
                .toList();
        return toDto(createOrder.execute(new NewOrder(orderRequest.userId(), lines)));
    }

    @Override
    public Order getOrder(Long id) {
        return toDto(getOrder.execute(id));
    }

    @Override
    public Order cancelOrder(Long id) {
        return toDto(cancelOrder.execute(id));
    }

    private static Order toDto(OrderView view) {
        List<OrderLine> lines = view.lines().stream().map(OrdersController::toDto).toList();
        return new Order(view.id(), view.userId(), Order.StatusEnum.valueOf(view.status()),
                view.createdAt().atOffset(ZoneOffset.UTC), view.total(), lines);
    }

    private static OrderLine toDto(OrderLineView line) {
        return new OrderLine(line.productId(), line.title(), line.unitPrice(), line.quantity());
    }
}

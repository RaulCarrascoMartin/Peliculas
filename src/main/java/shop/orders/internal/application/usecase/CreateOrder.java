package shop.orders.internal.application.usecase;

import shop.orders.internal.application.dto.NewOrder;
import shop.orders.internal.application.dto.OrderView;

import shop.orders.internal.domain.service.OrderService;
import shop.orders.internal.domain.contract.StockReservation;

public class CreateOrder {

    private final OrderService orders;

    public CreateOrder(OrderService orders) {
        this.orders = orders;
    }

    public OrderView execute(NewOrder request) {
        var items = request.lines().stream()
                .map(l -> new StockReservation.Item(l.productId(), l.quantity()))
                .toList();
        return OrderView.of(orders.create(request.userId(), items));
    }
}

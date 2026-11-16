package shop.orders.internal.application.usecase;

import shop.orders.internal.application.dto.OrderView;

import shop.orders.internal.domain.service.OrderService;

public class CancelOrder {

    private final OrderService orders;

    public CancelOrder(OrderService orders) {
        this.orders = orders;
    }

    public OrderView execute(Long id) {
        return OrderView.of(orders.cancel(id));
    }
}
